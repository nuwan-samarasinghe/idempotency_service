package com.example.idempotency.idempotency_service.common;

import ch.qos.logback.core.util.StringUtil;
import com.example.idempotency.idempotency_service.common.exceptions.IdempotancyException;
import com.example.idempotency.idempotency_service.models.IdempotencyModel;
import com.example.idempotency.idempotency_service.services.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.ObjectMapper;

@Aspect
@Component
public class IdempotencyAspect {

  public static final String IDEMPOTENCY_HEADER_KEY = "Idempotency-Key";

  private final IdempotencyService idempotencyService;
  private final ObjectMapper objectMapper;

  public IdempotencyAspect(IdempotencyService idempotencyService, ObjectMapper objectMapper) {
    this.idempotencyService = idempotencyService;
    this.objectMapper = objectMapper;
  }

  @Around("@annotation(com.example.idempotency.idempotency_service.common.Idempotent)")
  public Object applyIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {
    HttpServletRequest request = currentRequest();
    if (request != null) {
      // Only POST and PATCH are idempotent
      if (!HttpMethod.POST.matches(request.getMethod())
          && !HttpMethod.PATCH.matches(request.getMethod())) {
        return joinPoint.proceed();
      }
      String idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER_KEY);
      if (StringUtil.isNullOrEmpty(idempotencyKey)) {
        throw new IdempotancyException("No idempotency header found");
      }
      String cacheKey = buildCacheKey(request, idempotencyKey);
      IdempotencyModel idempotencyModel = idempotencyService.getCachedResponse(cacheKey);
      String requestHash = hash(serializeArgs(joinPoint.getArgs()));
      if (idempotencyModel == null) {
        Object result = joinPoint.proceed();
        if (result instanceof ResponseEntity<?> responseEntity) {
          idempotencyService.putResponse(cacheKey, responseEntity, requestHash);
        }
        idempotencyModel = idempotencyService.getCachedResponse(cacheKey);
      } else if (!idempotencyModel.getRequestHash().equals(requestHash)) {
        throw new IdempotancyException(
            "Idempotency key conflict: request data does not match previous request with the same key");
      }
      return idempotencyModel.toResponseEntity();
    } else {
      return joinPoint.proceed();
    }
  }

  private HttpServletRequest currentRequest() {
    RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
    if (!(attrs instanceof ServletRequestAttributes sra)) {
      return null;
    }
    return sra.getRequest();
  }

  private String buildCacheKey(HttpServletRequest request, String idempotencyKey) {
    return request.getMethod() + ":" + request.getRequestURI() + ":" + idempotencyKey;
  }

  private String serializeArgs(Object[] args) {
    return objectMapper.writeValueAsString(args);
  }

  String hash(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(bytes);
    } catch (NoSuchAlgorithmException ex) {
      throw new IdempotancyException("Request Hash generation failed", ex);
    }
  }
}
