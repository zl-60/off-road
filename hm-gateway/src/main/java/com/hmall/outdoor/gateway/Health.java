package com.hmall.outdoor.gateway;

import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
public class Health {

  @GetMapping("/health")
  public Map<String, Object> health() {
    return Map.of("status", "UP", "service", "gateway");
  }

  @GetMapping({ "/admin", "/admin/" })
  public org.springframework.http.ResponseEntity<Void> admin() {
    return org.springframework.http.ResponseEntity.status(302)
      .location(java.net.URI.create("/admin/index.html"))
      .build();
  }
}
