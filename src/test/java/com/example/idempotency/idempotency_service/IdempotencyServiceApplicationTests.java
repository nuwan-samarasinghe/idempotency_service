package com.example.idempotency.idempotency_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class IdempotencyServiceApplicationTests {

  @Test
  void contextLoads() {
    IdempotencyServiceApplication.main(new String[] {"--spring.main.web-application-type=none"});
  }
}
