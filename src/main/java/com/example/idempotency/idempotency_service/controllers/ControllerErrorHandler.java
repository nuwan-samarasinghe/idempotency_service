package com.example.idempotency.idempotency_service.controllers;

import com.example.idempotency.idempotency_service.common.exceptions.IdempotancyException;
import com.example.idempotency.idempotency_service.dtos.MessageDto;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ControllerErrorHandler {

  @ExceptionHandler(IdempotancyException.class)
  public ResponseEntity<MessageDto> handleNotFound(
      IdempotancyException ex, HttpServletRequest request) {
    return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
  }

  private ResponseEntity<MessageDto> buildError(
      HttpStatus status, String message, HttpServletRequest request) {
    MessageDto body =
        MessageDto.builder()
            .timestamp(Instant.now())
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(message)
            .path(request.getRequestURI())
            .build();
    return ResponseEntity.status(status).body(body);
  }

  /* More Exceptions can be handled here & for the minilast implementation, only IdempotancyException is handled.
   * Specially if we need to handle 404 Not Found or 500 Internal Server Error, we can add those handlers here.
   * Also a generic Exception handler can be added to catch any unhandled exceptions.
   * Also Database exceptions can be handled here if needed.
   */
}
