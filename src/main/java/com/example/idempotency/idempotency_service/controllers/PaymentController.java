package com.example.idempotency.idempotency_service.controllers;

import com.example.idempotency.idempotency_service.common.Idempotent;
import com.example.idempotency.idempotency_service.dtos.PaymentDto;
import com.example.idempotency.idempotency_service.services.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  // @GetMapping("/{id}") GET is idempotent by nature so not implementing that
  // @PutMapping("/{id}") PUT is idempotent by nature so not implementing that
  // @DeleteMapping("/{id}") DELETE is idempotent by nature so not implementing that

  @PostMapping("")
  @Idempotent
  public ResponseEntity<PaymentDto> createPayment(@RequestBody PaymentDto paymentDto) {
    return ResponseEntity.ok(this.paymentService.createPayment(paymentDto));
  }

  @PatchMapping("/{id}")
  @Idempotent
  public ResponseEntity<PaymentDto> partialUpdatePayment(
      @PathVariable String id, @RequestBody PaymentDto paymentDto) {
    return ResponseEntity.ok(this.paymentService.partialUpdatePayment(id, paymentDto));
  }
}
