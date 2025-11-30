package com.example.idempotency.idempotency_service.services;

import com.example.idempotency.idempotency_service.models.IdempotencyModel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

  private final Map<String, IdempotencyModel> cache = new ConcurrentHashMap<>();

  public IdempotencyModel getCachedResponse(String key) {
    IdempotencyModel entry = cache.get(key);
    if (entry == null) {
      return null;
    }
    return entry;
  }

  public void putResponse(String cacheKey, ResponseEntity<?> responseEntity, String requestHash) {
    cache.put(
        cacheKey,
        IdempotencyModel.builder()
            .response(responseEntity.getBody())
            .requestHash(requestHash)
            .status(responseEntity.getStatusCode())
            .headers(responseEntity.getHeaders())
            .build());
  }
}
