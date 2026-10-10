package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.dto.ClienteRequestDTO;
import com.proyecto.servicios.dto.ClienteResponseDTO;
import com.proyecto.servicios.entity.*;
import com.proyecto.servicios.exception.*;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.mapper.DomicilioMapper;
import com.proyecto.servicios.mapper.InformacionLaboralMapper;
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
import java.util.stream.Collectors;

/**
 * Implementación del servicio de Clientes (Persona Física).
 * Coordina las validaciones de duplicación, persistencia de cliente, domicilio e información laboral,
 * así como la creación automática de cuenta bancaria transaccional y operaciones de baja lógica.
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
        log.info("Iniciando onboarding para nuevo cliente con CURP: {}", dto.getCurp());

        // 1. Validación de mayoría de edad (mínimo 18 años cumplidos)
        if (dto.getFechaNacimiento() == null) {
            throw new ValidacionNegocioException("La fecha de nacimiento es obligatoria.");
        }
        if (dto.getFechaNacimiento().isAfter(LocalDate.now())) {
            throw new ValidacionNegocioException("La fecha de nacimiento no puede ser una fecha futura.");
        }
        if (Period.between(dto.getFechaNacimiento(), LocalDate.now()).getYears() < 18) {
            throw new ValidacionNegocioException("El cliente debe ser mayor de edad (mínimo 18 años) para realizar el registro.");
        }

        // 2. Validaciones de unicidad de negocio
        if (clienteRepository.existsByCurp(dto.getCurp())) {
            throw new CurpDuplicadaException("La CURP '" + dto.getCurp() + "' ya se encuentra registrada en el sistema.");
        }
        if (clienteRepository.existsByRfc(dto.getRfc())) {
            throw new RfcDuplicadoException("El RFC '" + dto.getRfc() + "' ya se encuentra registrado en el sistema.");
        }
        if (clienteRepository.existsByCorreo(dto.getCorreo())) {
            throw new ClienteYaExisteException("El correo electrónico '" + dto.getCorreo() + "' ya está registrado.");
        }

        // 3. Obtener o registrar catálogo de País y Estado Civil
        Pais nacionalidad = obtenerOCrearPais(dto.getNacionalidadId(), dto.getNacionalidad());
        EstadoCivil estadoCivil = obtenerOCrearEstadoCivil(dto.getEstadoCivilId(), dto.getEstadoCivil());

        // 4. Mapear DTO a Entidad Cliente
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
    public ClienteResponseDTO obtenerClientePorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró el cliente con ID: " + id));
        return ClienteMapper.toDTO(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> obtenerClientesActivos() {
        return clienteRepository.findByActivoTrue().stream()
                .map(cliente -> {
                    ClienteResponseDTO dto = ClienteMapper.toDTO(cliente);
                    dto.setId(null);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClienteResponseDTO actualizarCliente(Long id, ClienteRequestDTO dto) {
        log.info("Actualizando información del cliente ID={}", id);

        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró el cliente con ID: " + id));

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ValidacionNegocioException("No se puede actualizar un cliente que se encuentra dado de baja lógica.");
        }

        // Impedir modificación de CURP
        if (dto.getCurp() != null && !cliente.getCurp().equalsIgnoreCase(dto.getCurp())) {
            throw new ValidacionNegocioException("No está permitido modificar la CURP registrada del cliente.");
        }

        // Impedir modificación de RFC
        if (dto.getRfc() != null && !cliente.getRfc().equalsIgnoreCase(dto.getRfc())) {
            throw new ValidacionNegocioException("No está permitido modificar el RFC registrado del cliente.");
        }

        // Validar correo si cambió
        if (dto.getCorreo() != null && !cliente.getCorreo().equalsIgnoreCase(dto.getCorreo()) && clienteRepository.existsByCorreo(dto.getCorreo())) {
            throw new ClienteYaExisteException("El nuevo correo electrónico '" + dto.getCorreo() + "' ya está registrado por otro cliente.");
        }

        // Actualizar catalogos si cambian
        if (dto.getNacionalidadId() != null || dto.getNacionalidad() != null) {
            cliente.setNacionalidad(obtenerOCrearPais(dto.getNacionalidadId(), dto.getNacionalidad()));
        }
        if (dto.getEstadoCivilId() != null || dto.getEstadoCivil() != null) {
            cliente.setEstadoCivil(obtenerOCrearEstadoCivil(dto.getEstadoCivilId(), dto.getEstadoCivil()));
        }

        // Actualizar datos de contacto y personales
        cliente.setNombre(dto.getNombre());
        cliente.setSegundoNombre(dto.getSegundoNombre());
        cliente.setApellidoPaterno(dto.getApellidoPaterno());
        cliente.setApellidoMaterno(dto.getApellidoMaterno());
        cliente.setFechaNacimiento(dto.getFechaNacimiento());
        if (dto.getSexo() != null) cliente.setSexo(dto.getSexo());
        cliente.setCorreo(dto.getCorreo());
        cliente.setTelefonoMovil(dto.getTelefonoMovilFinal());
        cliente.setTelefonoAlternativo(dto.getTelefonoAlternativo());

        // Actualizar información laboral
        if (dto.getInformacionLaboral() != null) {
            InformacionLaboral infLab = cliente.getInformacionLaboral();
            if (infLab == null) {
                infLab = InformacionLaboralMapper.toEntity(dto.getInformacionLaboral(), cliente);
                cliente.setInformacionLaboral(infLab);
            } else {
                infLab.setOcupacion(dto.getInformacionLaboral().getOcupacion());
                infLab.setEmpresa(dto.getInformacionLaboral().getEmpresa());
                infLab.setIngresoMensual(dto.getInformacionLaboral().getIngresoMensual());
            }
        } else if (dto.getOcupacion() != null || dto.getEmpresa() != null || dto.getIngresoMensual() != null) {
            InformacionLaboral infLab = cliente.getInformacionLaboral();
            if (infLab == null) {
                infLab = InformacionLaboral.builder()
                        .cliente(cliente)
                        .ocupacion(dto.getOcupacion() != null ? dto.getOcupacion() : "NO ESPECIFICADO")
                        .empresa(dto.getEmpresa() != null ? dto.getEmpresa() : "NO ESPECIFICADO")
                        .ingresoMensual(dto.getIngresoMensual() != null ? dto.getIngresoMensual() : java.math.BigDecimal.ZERO)
                        .build();
                cliente.setInformacionLaboral(infLab);
            } else {
                if (dto.getOcupacion() != null) infLab.setOcupacion(dto.getOcupacion());
                if (dto.getEmpresa() != null) infLab.setEmpresa(dto.getEmpresa());
                if (dto.getIngresoMensual() != null) infLab.setIngresoMensual(dto.getIngresoMensual());
            }
        }

        // Actualizar domicilio
        if (dto.getDomicilio() != null) {
            Pais paisDom = obtenerOCrearPais(null, dto.getDomicilio().getPais());
            Domicilio domActual = cliente.getDomicilio();
            if (domActual == null) {
                domActual = DomicilioMapper.toEntity(dto.getDomicilio(), cliente, paisDom);
                cliente.setDomicilio(domActual);
            } else {
                domActual.setCalle(dto.getDomicilio().getCalle());
                domActual.setNumeroExterior(dto.getDomicilio().getNumeroExterior());
                domActual.setNumeroInterior(dto.getDomicilio().getNumeroInterior());
                domActual.setColonia(dto.getDomicilio().getColonia());
                domActual.setMunicipio(dto.getDomicilio().getMunicipio());
                domActual.setEstado(dto.getDomicilio().getEstado());
                domActual.setCodigoPostal(dto.getDomicilio().getCodigoPostal());
                domActual.setPais(paisDom);
            }
        }

        Cliente actualizado = clienteRepository.save(cliente);
        log.info("Cliente ID={} actualizado exitosamente", id);
        return ClienteMapper.toDTO(actualizado);
    }

    @Override
    @Transactional
    public void eliminarCliente(Long id) {
        log.info("Ejecutando baja lógica para el cliente ID={}", id);

        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró el cliente con ID: " + id));

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            log.warn("El cliente ID={} ya se encontraba en estado de baja lógica.", id);
            return;
        }

        // Marcar cliente como inactivo
        cliente.setActivo(false);
        cliente.setFechaBaja(LocalDateTime.now());
        clienteRepository.save(cliente);

        // Desactivar cuentas bancarias asociadas mediante estatus_cuenta
        EstatusCuenta estatusInactiva = estatusCuentaRepository.findByNombreIgnoreCase("INACTIVA")
                .orElseGet(() -> estatusCuentaRepository.save(EstatusCuenta.builder().nombre("INACTIVA").descripcion("Cuenta Inactiva").activo(true).build()));

        List<Cuenta> cuentas = cuentaRepository.findByClienteId(id);
        for (Cuenta cuenta : cuentas) {
            cuenta.setEstatusCuenta(estatusInactiva);
            cuentaRepository.save(cuenta);
        }

        log.info("Baja lógica completada para cliente ID={} y sus {} cuentas asociadas", id, cuentas.size());
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorCurp(String curp) {
        Cliente cliente = clienteRepository.findByCurp(curp)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró ningún cliente registrado con la CURP: " + curp));
        return ClienteMapper.toDTO(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorRfc(String rfc) {
        Cliente cliente = clienteRepository.findByRfc(rfc)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró ningún cliente registrado con el RFC: " + rfc));
        return ClienteMapper.toDTO(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorCorreo(String correo) {
        Cliente cliente = clienteRepository.findByCorreo(correo)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró ningún cliente registrado con el correo: " + correo));
        return ClienteMapper.toDTO(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> buscarPorFechaCreacion(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            throw new ValidacionNegocioException("Las fechas de inicio y fin son obligatorias para el filtro por rango.");
        }
        if (fechaInicio.isAfter(fechaFin)) {
            throw new ValidacionNegocioException("La fecha de inicio debe ser menor o igual a la fecha de fin.");
        }

        return clienteRepository.findByActivoTrueAndFechaCreacionBetween(fechaInicio, fechaFin).stream()
                .map(ClienteMapper::toDTO)
                .collect(Collectors.toList());
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
