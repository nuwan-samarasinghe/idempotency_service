package com.example.idempotency.idempotency_service.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.idempotency.idempotency_service.common.exceptions.IdempotancyException;
import com.example.idempotency.idempotency_service.services.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class TestIdempotencyAspect {

  @Mock private IdempotencyService idempotencyService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock private ProceedingJoinPoint joinPoint;

  private IdempotencyAspect aspect;

  @BeforeEach
  void setUp() {
    aspect = new IdempotencyAspect(idempotencyService, objectMapper);
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void whenNoRequestAttributes_thenProceed() throws Throwable {
    when(joinPoint.proceed()).thenReturn("result");
    Object result = aspect.applyIdempotency(joinPoint);
    assertEquals("result", result);
    verifyNoInteractions(idempotencyService);
  }

  @Test
  void whenMethodIsGet_thenProceedWithoutIdempotency() throws Throwable {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn("GET");
    setRequest(request);
    when(joinPoint.proceed()).thenReturn("result");
    Object result = aspect.applyIdempotency(joinPoint);
    assertEquals("result", result);
    verifyNoInteractions(idempotencyService);
  }

  @Test
  void whenPostWithoutIdempotencyKey_thenThrow() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn("POST");
    when(request.getHeader(IdempotencyAspect.IDEMPOTENCY_HEADER_KEY)).thenReturn(null);
    setRequest(request);
    assertThrows(IdempotancyException.class, () -> aspect.applyIdempotency(joinPoint));
    verifyNoInteractions(idempotencyService);
  }

  @Test
  void hash_whenAlgorithmMissing_shouldWrapInIdempotancyException() {
    IdempotencyAspect aspect = new IdempotencyAspect(idempotencyService, new ObjectMapper());
    try (MockedStatic<MessageDigest> mocked = mockStatic(MessageDigest.class)) {
      mocked
          .when(() -> MessageDigest.getInstance("SHA-256"))
          .thenThrow(new NoSuchAlgorithmException("No algorithm"));
      IdempotancyException ex =
          assertThrows(IdempotancyException.class, () -> aspect.hash("input"));
      assertEquals("Request Hash generation failed", ex.getMessage());
      assertTrue(ex.getCause() instanceof NoSuchAlgorithmException);
    }
  }

  private void setRequest(HttpServletRequest request) {
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }
}
