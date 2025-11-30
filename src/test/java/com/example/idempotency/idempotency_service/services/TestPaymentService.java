package com.example.idempotency.idempotency_service.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.idempotency.idempotency_service.common.exceptions.IdempotancyException;
import com.example.idempotency.idempotency_service.dtos.PaymentDto;
import com.example.idempotency.idempotency_service.repositories.PaymentRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TestPaymentService {

  @Mock private PaymentRepository paymentRepository;

  private PaymentService paymentService;

  @BeforeEach
  void setUp() {
    paymentService = new PaymentService(paymentRepository);
  }

  @Test
  void createPayment_whenAmountIsNull_throwsException() {
    PaymentDto dto = PaymentDto.builder().amount(null).currency("EUR").build();
    IdempotancyException ex =
        assertThrows(IdempotancyException.class, () -> paymentService.createPayment(dto));
    assertEquals("Amount and Currency are required fields", ex.getMessage());
    verifyNoInteractions(paymentRepository);
  }

  @Test
  void createPayment_whenCurrencyIsNull_throwsException() {
    PaymentDto dto = PaymentDto.builder().amount(BigDecimal.TEN).currency(null).build();
    IdempotancyException ex =
        assertThrows(IdempotancyException.class, () -> paymentService.createPayment(dto));
    assertEquals("Amount and Currency are required fields", ex.getMessage());
    verifyNoInteractions(paymentRepository);
  }
}
