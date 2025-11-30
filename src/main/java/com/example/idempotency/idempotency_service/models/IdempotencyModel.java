package com.example.idempotency.idempotency_service.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyModel {

  private String requestHash;
  private Object response;
  private HttpStatusCode status;
  private HttpHeaders headers;

  public ResponseEntity<Object> toResponseEntity() {
    HttpHeaders copy = new HttpHeaders();
    headers.headerSet().forEach(entry -> copy.put(entry.getKey(), entry.getValue()));
    return new ResponseEntity<>(response, copy, status);
  }
}
