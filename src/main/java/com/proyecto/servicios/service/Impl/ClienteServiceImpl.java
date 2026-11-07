package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.dto.ClienteBajaRequestDTO;
import com.proyecto.servicios.dto.ClienteFiltroRequestDTO;
import com.proyecto.servicios.dto.ClienteRequestDTO;
import com.proyecto.servicios.dto.ClienteResponseDTO;
import com.proyecto.servicios.entity.*;
import com.proyecto.servicios.exception.*;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.mapper.DomicilioMapper;
import com.proyecto.servicios.repositorys.*;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.service.CuentaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Locale;

/**
 * Implementación del servicio de Clientes (Usuarios).
 * Coordina las validaciones de duplicación, persistencia de cliente, domicilio e información laboral,
 * la creación automática de cuenta bancaria, la búsqueda dinámica por filtros y la baja lógica.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final PaisRepository paisRepository;
    private final EstadoCivilRepository estadoCivilRepository;
    private final EstatusCuentaRepository estatusCuentaRepository;
    private final CuentaService cuentaService;

    @Override
    @Transactional
    public ClienteResponseDTO crearCliente(ClienteRequestDTO dto) {
        log.info("Iniciando registro de nuevo cliente");
        dto.setCorreo(normalizarCorreo(dto.getCorreo()));

        // 1. Validación de mayoría de edad (mínimo 18 años)
        if (Period.between(dto.getFechaNacimiento(), LocalDate.now()).getYears() < 18) {
            throw new ValidacionNegocioException("El cliente debe ser mayor de edad (mínimo 18 años) para realizar el registro.");
        }

        // 2. Validaciones de unicidad de negocio (la BD mantiene además restricciones UNIQUE ante concurrencia)
        if (clienteRepository.existsByCurp(dto.getCurp())) {
            throw new CurpDuplicadaException("La CURP '" + dto.getCurp() + "' ya se encuentra registrada en el sistema.");
        }
        if (clienteRepository.existsByRfc(dto.getRfc())) {
            throw new RfcDuplicadoException("El RFC '" + dto.getRfc() + "' ya se encuentra registrado en el sistema.");
        }
        if (clienteRepository.existsByCorreoIgnoreCase(dto.getCorreo())) {
            throw new ClienteYaExisteException("El correo electrónico '" + dto.getCorreo() + "' ya está registrado.");
        }

        // 3. Obtener o registrar catálogo de País y Estado Civil
        Pais nacionalidad = obtenerOCrearPais(dto.getNacionalidadId(), dto.getNacionalidad());
        EstadoCivil estadoCivil = obtenerOCrearEstadoCivil(dto.getEstadoCivilId(), dto.getEstadoCivil());

        // 4. Mapear DTO a Entidad Cliente (id, activo y marcas de tiempo son asignados por el servidor)
        Cliente cliente = ClienteMapper.toEntity(dto, nacionalidad, estadoCivil);

        // 5. Mapear Domicilio
        if (dto.getDomicilio() != null) {
            Pais paisDomicilio = obtenerOCrearPais(null, dto.getDomicilio().getPais());
            Domicilio domicilio = DomicilioMapper.toEntity(dto.getDomicilio(), cliente, paisDomicilio);
            cliente.setDomicilio(domicilio);
        }

        // 6. Persistir Cliente (con Cascade Domicilio e InformacionLaboral)
        Cliente clienteGuardado = clienteRepository.save(cliente);

        // 7. Crear automáticamente la cuenta bancaria y su saldo inicial
        Cuenta cuentaAutomatica = cuentaService.crearCuentaParaCliente(clienteGuardado);
        clienteGuardado.getCuentas().add(cuentaAutomatica);

        log.info("Cliente registrado exitosamente con ID={} y Cuenta N°={}", clienteGuardado.getId(), cuentaAutomatica.getNumeroCuenta());
        return ClienteMapper.toDTO(clienteGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> buscarClientes(ClienteFiltroRequestDTO filtro) {
        return ClienteMapper.toDTO(clienteRepository.findAll(ClienteSpecifications.construirCriterios(filtro)));
    }

    /**
     * Sobrecarga de búsqueda por nombre y correo:
     * Reutiliza la búsqueda dinámica delegando en buscarClientes(ClienteFiltroRequestDTO).
     */
    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> buscarClientes(String nombre, String correo) {
        log.info("Ejecutando búsqueda sobrecargada por nombre='{}' y correo='{}'", nombre, correo);
        return buscarClientes(ClienteFiltroRequestDTO.builder()
                .nombre(nombre)
                .correo(correo)
                .build());
    }

    @Override
    @Transactional
    public ClienteResponseDTO darDeBaja(ClienteBajaRequestDTO request) {
        Long id = request.getId();
        log.info("Ejecutando baja lógica para el cliente ID={}", id);

        Cliente cliente = obtenerClienteActivo(id);

        // Marcar cliente como inactivo (nunca se elimina físicamente)
        cliente.setActivo(false);
        cliente.setFechaBaja(LocalDateTime.now());
        Cliente actualizado = clienteRepository.save(cliente);

        // Desactivar cuentas bancarias asociadas mediante estatus_cuenta
        EstatusCuenta estatusInactiva = estatusCuentaRepository.findByNombreIgnoreCase("INACTIVA")
                .orElseGet(() -> estatusCuentaRepository.save(EstatusCuenta.builder()
                        .nombre("INACTIVA")
                        .descripcion("Cuenta Inactiva")
                        .activo(true)
                        .build()));

        List<Cuenta> cuentas = cuentaRepository.findByClienteId(id);
        for (Cuenta cuenta : cuentas) {
            cuenta.setEstatusCuenta(estatusInactiva);
            cuentaRepository.save(cuenta);
        }

        log.info("Baja lógica completada para cliente ID={} y sus {} cuentas asociadas", id, cuentas.size());
        return ClienteMapper.toDTO(actualizado);
    }

    /**
     * Método reutilizable: recupera un cliente garantizando que existe y que está activo.
     * Centraliza las comprobaciones de existencia y estado en un único punto.
     */
    Cliente obtenerClienteActivo(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró el cliente con ID: " + id));
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ClienteYaEliminadoException("El cliente con ID: " + id + " ya se encuentra dado de baja.");
        }
        return cliente;
    }

    /**
     * Método reutilizable: normaliza el correo (recorte de espacios y minúsculas)
     * para asegurar comparaciones y persistencia homogéneas.
     */
    static String normalizarCorreo(String correo) {
        String valor = ClienteSpecifications.normalizar(correo);
        return valor == null ? null : valor.toLowerCase(Locale.ROOT);
    }

    private Pais obtenerOCrearPais(Long id, String nombre) {
        if (id != null) {
            var paisOpt = paisRepository.findById(id);
            if (paisOpt.isPresent()) {
                return paisOpt.get();
            }
        }

        String nombrePais = (nombre != null && !nombre.isBlank()) ? nombre.trim() : "MÉXICO";
        if ("MEXICANA".equalsIgnoreCase(nombrePais) || "MEXICANO".equalsIgnoreCase(nombrePais) || "MEXICO".equalsIgnoreCase(nombrePais)) {
            nombrePais = "MÉXICO";
        }

        var optPorNombre = paisRepository.findByNombreIgnoreCase(nombrePais);
        if (optPorNombre.isPresent()) {
            return optPorNombre.get();
        }

        String iso = nombrePais.length() >= 3 ? nombrePais.substring(0, 3).toUpperCase() : "MEX";
        var optPorIso = paisRepository.findByCodigoIsoIgnoreCase(iso);
        if (optPorIso.isPresent()) {
            return optPorIso.get();
        }

        return paisRepository.save(Pais.builder()
                .nombre(nombrePais)
                .codigoIso(iso)
                .activo(true)
                .build());
    }

    private EstadoCivil obtenerOCrearEstadoCivil(Long id, String descripcion) {
        if (id != null) {
            var ecOpt = estadoCivilRepository.findById(id);
            if (ecOpt.isPresent()) {
                return ecOpt.get();
            }
        }

        String desc = (descripcion != null && !descripcion.isBlank()) ? descripcion.trim() : "SOLTERO(A)";

        var optDirecto = estadoCivilRepository.findByDescripcionIgnoreCase(desc);
        if (optDirecto.isPresent()) {
            return optDirecto.get();
        }

        String descNormalizada = desc;
        if ("SOLTERO".equalsIgnoreCase(desc) || "SOLTERA".equalsIgnoreCase(desc)) descNormalizada = "SOLTERO(A)";
        else if ("CASADO".equalsIgnoreCase(desc) || "CASADA".equalsIgnoreCase(desc)) descNormalizada = "CASADO(A)";
        else if ("DIVORCIADO".equalsIgnoreCase(desc) || "DIVORCIADA".equalsIgnoreCase(desc)) descNormalizada = "DIVORCIADO(A)";
        else if ("VIUDO".equalsIgnoreCase(desc) || "VIUDA".equalsIgnoreCase(desc)) descNormalizada = "VIUDO(A)";

        return estadoCivilRepository.findByDescripcionIgnoreCase(descNormalizada)
                .orElseGet(() -> estadoCivilRepository.save(EstadoCivil.builder()
                        .descripcion(desc)
                        .activo(true)
                        .build()));
    }
}
