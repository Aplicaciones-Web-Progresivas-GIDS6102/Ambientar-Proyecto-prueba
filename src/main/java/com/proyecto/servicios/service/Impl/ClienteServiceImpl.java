package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.dto.ClienteRequestDTO;
import com.proyecto.servicios.dto.ClienteResponseDTO;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.entity.Domicilio;
import com.proyecto.servicios.enums.EstadoCuenta;
import com.proyecto.servicios.exception.*;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.mapper.DomicilioMapper;
import com.proyecto.servicios.repositorys.ClienteRepository;
import com.proyecto.servicios.repositorys.CuentaRepository;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.service.CuentaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de Clientes (Persona Física).
 * Coordina las validaciones de duplicación, persistencia de cliente y domicilio,
 * creación automática de cuenta bancaria transaccional y operaciones de baja lógica.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final CuentaService cuentaService;

    @Override
    @Transactional
    public ClienteResponseDTO crearCliente(ClienteRequestDTO dto) {
        log.info("Iniciando onboarding para nuevo cliente con CURP: {}", dto.getCurp());

        // 1. Validaciones de unicidad de negocio
        if (clienteRepository.existsByCurp(dto.getCurp())) {
            throw new CurpDuplicadaException("La CURP '" + dto.getCurp() + "' ya se encuentra registrada en el sistema.");
        }
        if (clienteRepository.existsByRfc(dto.getRfc())) {
            throw new RfcDuplicadoException("El RFC '" + dto.getRfc() + "' ya se encuentra registrado en el sistema.");
        }
        if (clienteRepository.existsByCorreo(dto.getCorreo())) {
            throw new ClienteYaExisteException("El correo electrónico '" + dto.getCorreo() + "' ya está registrado.");
        }

        // 2. Mapear DTO a Entidad Cliente y Domicilio
        Cliente cliente = ClienteMapper.toEntity(dto);
        Domicilio domicilio = DomicilioMapper.toEntity(dto.getDomicilio(), cliente);
        cliente.setDomicilio(domicilio);

        // 3. Persistir Cliente y Domicilio
        Cliente clienteGuardado = clienteRepository.save(cliente);

        // 4. Crear automáticamente la cuenta bancaria y su saldo inicial
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
                .map(ClienteMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClienteResponseDTO actualizarCliente(Long id, ClienteRequestDTO dto) {
        log.info("Actualizando información del cliente ID={}", id);

        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró el cliente con ID: " + id));

        if (Boolean.TRUE.equals(cliente.getEliminado())) {
            throw new ValidacionNegocioException("No se puede actualizar un cliente que se encuentra dado de baja lógica.");
        }

        // Impedir modificación de CURP
        if (!cliente.getCurp().equalsIgnoreCase(dto.getCurp())) {
            throw new ValidacionNegocioException("No está permitido modificar la CURP registrada del cliente.");
        }

        // Impedir modificación de RFC
        if (!cliente.getRfc().equalsIgnoreCase(dto.getRfc())) {
            throw new ValidacionNegocioException("No está permitido modificar el RFC registrado del cliente.");
        }

        // Validar correo si cambió
        if (!cliente.getCorreo().equalsIgnoreCase(dto.getCorreo()) && clienteRepository.existsByCorreo(dto.getCorreo())) {
            throw new ClienteYaExisteException("El nuevo correo electrónico '" + dto.getCorreo() + "' ya está registrado por otro cliente.");
        }

        // Actualizar datos del cliente
        cliente.setNombre(dto.getNombre());
        cliente.setSegundoNombre(dto.getSegundoNombre());
        cliente.setApellidoPaterno(dto.getApellidoPaterno());
        cliente.setApellidoMaterno(dto.getApellidoMaterno());
        cliente.setFechaNacimiento(dto.getFechaNacimiento());
        cliente.setSexo(dto.getSexo());
        if (dto.getNacionalidad() != null) cliente.setNacionalidad(dto.getNacionalidad());
        cliente.setEstadoCivil(dto.getEstadoCivil());
        cliente.setCorreo(dto.getCorreo());
        cliente.setMovil(dto.getMovil());
        cliente.setTelefonoAlternativo(dto.getTelefonoAlternativo());
        cliente.setOcupacion(dto.getOcupacion());
        cliente.setEmpresa(dto.getEmpresa());
        cliente.setIngresoMensual(dto.getIngresoMensual());

        // Actualizar domicilio
        if (dto.getDomicilio() != null) {
            Domicilio domActual = cliente.getDomicilio();
            if (domActual == null) {
                domActual = DomicilioMapper.toEntity(dto.getDomicilio(), cliente);
                cliente.setDomicilio(domActual);
            } else {
                domActual.setCalle(dto.getDomicilio().getCalle());
                domActual.setNumeroExterior(dto.getDomicilio().getNumeroExterior());
                domActual.setNumeroInterior(dto.getDomicilio().getNumeroInterior());
                domActual.setColonia(dto.getDomicilio().getColonia());
                domActual.setMunicipio(dto.getDomicilio().getMunicipio());
                domActual.setEstado(dto.getDomicilio().getEstado());
                domActual.setCodigoPostal(dto.getDomicilio().getCodigoPostal());
                if (dto.getDomicilio().getPais() != null) {
                    domActual.setPais(dto.getDomicilio().getPais());
                }
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

        if (Boolean.TRUE.equals(cliente.getEliminado())) {
            log.warn("El cliente ID={} ya se encontraba en estado de baja lógica.", id);
            return;
        }

        // Marcar cliente como inactivo y eliminado
        cliente.setActivo(false);
        cliente.setEliminado(true);
        cliente.setFechaBaja(LocalDateTime.now());
        clienteRepository.save(cliente);

        // Desactivar cuentas bancarias asociadas
        List<Cuenta> cuentas = cuentaRepository.findByClienteId(id);
        for (Cuenta cuenta : cuentas) {
            cuenta.setActivo(false);
            cuenta.setEstado(EstadoCuenta.INACTIVA);
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
}
