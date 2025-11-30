package com.example.idempotency.idempotency_service.common.exceptions;

public class IdempotancyException extends RuntimeException {

  public IdempotancyException(String message) {
    super(message);
  }

  public IdempotancyException(String message, Throwable cause) {
    super(message, cause);
  }
}
