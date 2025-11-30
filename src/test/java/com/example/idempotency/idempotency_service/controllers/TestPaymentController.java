package com.example.idempotency.idempotency_service.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.idempotency.idempotency_service.common.IdempotencyAspect;
import com.example.idempotency.idempotency_service.dtos.PaymentDto;
import com.example.idempotency.idempotency_service.models.Payment;
import com.example.idempotency.idempotency_service.repositories.PaymentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

class TestPaymentController extends TestBaseController {

  @Autowired private PaymentRepository paymentRepository;
  private final ObjectMapper objectMapper = new ObjectMapper();

  // -- Create Payment Tests -- //
  // When same request is sent with same Idempotent key it should return the same response
  @Test
  void createPayment_sameIdempotencyKeyAndSameRequest_isIdempotent() throws Exception {
    PaymentDto dto = PaymentDto.builder().amount(BigDecimal.valueOf(40.00)).currency("SMS").build();

    MvcResult result1 =
        this.mockMvc
            .perform(
                post("/api/payments")
                    .header(IdempotencyAspect.IDEMPOTENCY_HEADER_KEY, "unique-key-123")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amount").value(dto.getAmount().doubleValue()))
            .andExpect(jsonPath("$.currency").value(dto.getCurrency()))
            .andReturn();

    MvcResult result2 =
        this.mockMvc
            .perform(
                post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(IdempotencyAspect.IDEMPOTENCY_HEADER_KEY, "unique-key-123")
                    .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amount").value(dto.getAmount().doubleValue()))
            .andExpect(jsonPath("$.currency").value(dto.getCurrency()))
            .andReturn();

    Assertions.assertThat(
            paymentRepository.findAll().stream()
                .filter(
                    p ->
                        p.getAmount().compareTo(dto.getAmount()) == 0
                            && p.getCurrency().equals(dto.getCurrency()))
                .count())
        .isEqualTo(1L);

    Assertions.assertThat(result1.getResponse().getContentAsString())
        .isEqualTo(result2.getResponse().getContentAsString());
  }

  // When same idempotent key is used with different request data it should return an error
  @Test
  void createPayment_sameIdempotencyKeyAndDifferentRequest_returnsClientError() throws Exception {
    PaymentDto dto = PaymentDto.builder().amount(BigDecimal.valueOf(50.00)).currency("SMS").build();
    this.mockMvc
        .perform(
            post("/api/payments")
                .header(IdempotencyAspect.IDEMPOTENCY_HEADER_KEY, "unique-key-124")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value(dto.getAmount().doubleValue()))
        .andExpect(jsonPath("$.currency").value(dto.getCurrency()));

    PaymentDto dto2 =
        PaymentDto.builder().amount(BigDecimal.valueOf(55.00)).currency("DDD").build();
    this.mockMvc
        .perform(
            post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .header(IdempotencyAspect.IDEMPOTENCY_HEADER_KEY, "unique-key-124")
                .content(objectMapper.writeValueAsString(dto2)))
        .andExpect(status().is4xxClientError());
  }

  // When different idempotent keys are used with same request (not treating as same request since
  // same payment data can sent) data it should create new resources
  @Test
  void createPayment_differentIdempotencyKeyAndSameRequest_createsAnotherPayment()
      throws Exception {
    PaymentDto dto = PaymentDto.builder().amount(BigDecimal.valueOf(60.00)).currency("SMS").build();
    this.mockMvc
        .perform(
            post("/api/payments")
                .header(IdempotencyAspect.IDEMPOTENCY_HEADER_KEY, "unique-key-125")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value(dto.getAmount().doubleValue()))
        .andExpect(jsonPath("$.currency").value(dto.getCurrency()));

    PaymentDto dto2 =
        PaymentDto.builder().amount(BigDecimal.valueOf(65.00)).currency("DDD").build();
    this.mockMvc
        .perform(
            post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .header(IdempotencyAspect.IDEMPOTENCY_HEADER_KEY, "unique-key-126")
                .content(objectMapper.writeValueAsString(dto2)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value(dto2.getAmount().doubleValue()))
        .andExpect(jsonPath("$.currency").value(dto2.getCurrency()));
  }

  // When idempotent key is missing it should return an error
  @Test
  void createPayment_missingIdempotencyKey_returnsClientError() throws Exception {
    PaymentDto dto = PaymentDto.builder().amount(BigDecimal.valueOf(70.00)).currency("SMS").build();
    this.mockMvc
        .perform(
            post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().is4xxClientError());
  }

  // Validation Tests
  @Test
  void createPayment_missingRequiredField_returnsValidationError() throws Exception {
    PaymentDto dto = PaymentDto.builder().amount(BigDecimal.valueOf(80.00)).build();
    this.mockMvc
        .perform(
            post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().is4xxClientError());

    PaymentDto dto2 = PaymentDto.builder().currency("FFF").build();
    this.mockMvc
        .perform(
            post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto2)))
        .andExpect(status().is4xxClientError());

    PaymentDto dto3 =
        PaymentDto.builder().amount(BigDecimal.valueOf(90.00)).currency("DDDDDDDDD").build();
    this.mockMvc
        .perform(
            post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto3)))
        .andExpect(status().is4xxClientError());
  }

  // -- Partial Update Payment Tests -- //
  // When same request is sent with same Idempotent key it should return the same response
  @Test
  void partialUpdatePayment_sameIdempotencyKeyAndSameRequest_isIdempotent() throws Exception {
    Payment pay =
        paymentRepository.saveAndFlush(
            Payment.builder()
                .amount(BigDecimal.valueOf(100.00))
                .currency("GBP")
                .createdAt(Instant.now())
                .build());

    PaymentDto dto = PaymentDto.builder().amount(BigDecimal.valueOf(105.00)).build();
    MvcResult result1 =
        this.mockMvc
            .perform(
                patch("/api/payments/" + pay.getId().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(IdempotencyAspect.IDEMPOTENCY_HEADER_KEY, "patch-unique-key-123")
                    .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(pay.getId().toString()))
            .andExpect(jsonPath("$.amount").value(dto.getAmount().doubleValue()))
            .andExpect(jsonPath("$.currency").value(pay.getCurrency()))
            .andReturn();

    MvcResult result2 =
        this.mockMvc
            .perform(
                patch("/api/payments/" + pay.getId().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(IdempotencyAspect.IDEMPOTENCY_HEADER_KEY, "patch-unique-key-123")
                    .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(pay.getId().toString()))
            .andExpect(jsonPath("$.amount").value(dto.getAmount().doubleValue()))
            .andExpect(jsonPath("$.currency").value(pay.getCurrency()))
            .andReturn();

    Assertions.assertThat(
            paymentRepository.findAll().stream()
                .filter(
                    p ->
                        p.getAmount().compareTo(dto.getAmount()) == 0
                            && p.getCurrency().equals(pay.getCurrency()))
                .count())
        .isEqualTo(1L);

    Assertions.assertThat(result1.getResponse().getContentAsString())
        .isEqualTo(result2.getResponse().getContentAsString());
  }

  // When same idempotent key is used with different request data it should return an error
  @Test
  void partialUpdatePayment_sameIdempotencyKeyAndDifferentRequest_returnsClientError()
      throws Exception {
    Payment pay =
        paymentRepository.saveAndFlush(
            Payment.builder()
                .amount(BigDecimal.valueOf(110.00))
                .currency("GBP")
                .createdAt(Instant.now())
                .build());

    PaymentDto dto = PaymentDto.builder().amount(BigDecimal.valueOf(120.00)).build();
    this.mockMvc
        .perform(
            patch("/api/payments/" + pay.getId().toString())
                .header(IdempotencyAspect.IDEMPOTENCY_HEADER_KEY, "patch-unique-key-124")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value(dto.getAmount().doubleValue()))
        .andExpect(jsonPath("$.currency").value(pay.getCurrency()));

    PaymentDto dto2 = PaymentDto.builder().amount(BigDecimal.valueOf(125.00)).build();
    this.mockMvc
        .perform(
            patch("/api/payments/" + pay.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .header(IdempotencyAspect.IDEMPOTENCY_HEADER_KEY, "patch-unique-key-124")
                .content(objectMapper.writeValueAsString(dto2)))
        .andExpect(status().is4xxClientError());
  }

  // When different idempotent keys are used with same request (not treating as same request since
  // same payment data can sent) data it should create new resources
  @Test
  void partialUpdatePayment_differentIdempotencyKeyAndSameRequest_partialUpdateAnotherPayment()
      throws Exception {
    Payment pay =
        paymentRepository.saveAndFlush(
            Payment.builder()
                .amount(BigDecimal.valueOf(210.00))
                .currency("GBP")
                .createdAt(Instant.now())
                .build());

    PaymentDto dto = PaymentDto.builder().amount(BigDecimal.valueOf(220.00)).build();
    this.mockMvc
        .perform(
            patch("/api/payments/" + pay.getId().toString())
                .header(IdempotencyAspect.IDEMPOTENCY_HEADER_KEY, "patch-unique-key-125")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value(dto.getAmount().doubleValue()))
        .andExpect(jsonPath("$.currency").value(pay.getCurrency()));

    PaymentDto dto2 = PaymentDto.builder().amount(BigDecimal.valueOf(225.00)).build();
    this.mockMvc
        .perform(
            patch("/api/payments/" + pay.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .header(IdempotencyAspect.IDEMPOTENCY_HEADER_KEY, "patch-unique-key-126")
                .content(objectMapper.writeValueAsString(dto2)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value(dto2.getAmount().doubleValue()))
        .andExpect(jsonPath("$.currency").value(pay.getCurrency()));
  }

  @Test
  void partialUpdatePayment_missingIdempotencyKey_returnsClientError() throws Exception {
    Payment pay =
        paymentRepository.saveAndFlush(
            Payment.builder()
                .amount(BigDecimal.valueOf(300.00))
                .currency("GBP")
                .createdAt(Instant.now())
                .build());

    PaymentDto dto =
        PaymentDto.builder().amount(BigDecimal.valueOf(305.00)).currency("SMS").build();
    this.mockMvc
        .perform(
            patch("/api/payments/" + pay.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().is4xxClientError());
  }
}
