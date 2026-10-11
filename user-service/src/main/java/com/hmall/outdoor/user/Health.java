package com.hmall.outdoor.user;

import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
public class Health {

  @GetMapping("/health")
  public Map<String, Object> health() {
    return Map.of("status", "UP", "service", "user");
  }
}
