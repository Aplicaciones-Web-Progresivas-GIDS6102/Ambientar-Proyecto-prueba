package com.proyecto.servicios.repositorys.gestopago;

import com.proyecto.servicios.entity.gestopago.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByClienteIdAndConsumedFalseOrderByIdDesc(Long clienteId);

    Optional<OtpVerification> findTopByClienteIdAndOperacionHashAndConsumedFalseOrderByIdDesc(Long clienteId, String operacionHash);

    Optional<OtpVerification> findByClienteIdAndOtpHashAndConsumedFalse(Long clienteId, String otpHash);

    List<OtpVerification> findAllByClienteIdAndConsumedFalse(Long clienteId);
}
