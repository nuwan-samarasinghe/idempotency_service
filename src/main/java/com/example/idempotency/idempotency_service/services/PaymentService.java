package com.example.idempotency.idempotency_service.services;

import com.example.idempotency.idempotency_service.common.exceptions.IdempotancyException;
import com.example.idempotency.idempotency_service.dtos.PaymentDto;
import com.example.idempotency.idempotency_service.models.Payment;
import com.example.idempotency.idempotency_service.repositories.PaymentRepository;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

  private final PaymentRepository paymentRepository;

  public PaymentService(PaymentRepository paymentRepository) {
    this.paymentRepository = paymentRepository;
  }

  @Transactional
  public PaymentDto createPayment(PaymentDto paymentDto) {
    if (paymentDto.getAmount() == null || paymentDto.getCurrency() == null) {
      throw new IdempotancyException("Amount and Currency are required fields");
    }
    Payment savedPayment =
        this.paymentRepository.saveAndFlush(
            Payment.builder()
                .amount(paymentDto.getAmount())
                .currency(paymentDto.getCurrency())
                .createdAt(Instant.now())
                .build());
    return PaymentDto.builder()
        .amount(savedPayment.getAmount())
        .currency(savedPayment.getCurrency())
        .build();
  }

  @Transactional
  public PaymentDto partialUpdatePayment(String id, PaymentDto paymentDto) {
    Payment payment =
        this.paymentRepository
            .findById(UUID.fromString(id))
            .orElseThrow(() -> new IdempotancyException("Payment not found"));
    Optional.ofNullable(paymentDto.getAmount()).ifPresent(payment::setAmount);
    Optional.ofNullable(paymentDto.getCurrency()).ifPresent(payment::setCurrency);
    Payment updatedPayment = this.paymentRepository.saveAndFlush(payment);
    return PaymentDto.builder()
        .id(updatedPayment.getId())
        .amount(updatedPayment.getAmount())
        .currency(updatedPayment.getCurrency())
        .build();
  }
}
