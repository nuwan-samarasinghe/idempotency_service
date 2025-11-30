package com.example.idempotency.idempotency_service.repositories;

import com.example.idempotency.idempotency_service.models.Payment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {}
