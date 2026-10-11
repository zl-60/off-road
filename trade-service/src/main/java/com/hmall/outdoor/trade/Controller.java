package com.hmall.outdoor.trade;

import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class Controller {

  private final Orders o;

  public Controller(Orders o) {
    this.o = o;
  }

  @PostMapping("/orders")
  public Object create(@RequestBody Map<String, Object> b) {
    return o.create(b);
  }

  @GetMapping("/orders")
  public Object list() {
    return o.list(false);
  }

  @GetMapping("/orders/{id}")
  public Object detail(@PathVariable String id) {
    return o.detail(id, false);
  }

  @PostMapping("/orders/{id}/{action}")
  public Object command(
    @PathVariable String id,
    @PathVariable String action,
    @RequestBody(required = false) Map<String, Object> b
  ) {
    return o.command(id, action, b == null ? Map.of() : b);
  }

  @GetMapping("/admin/orders")
  public Object admin() {
    return o.list(true);
  }

  @PostMapping("/admin/orders/{id}/ship")
  public Object ship(@PathVariable String id, @RequestBody Map<String, Object> b) {
    return o.command(id, "ship", b);
  }

  @GetMapping("/admin/dashboard")
  public Object stats() {
    return o.dashboard();
  }

  @PostMapping("/internal/payment-events")
  public Object event(@RequestBody Map<String, Object> b) {
    return o.complete(
      com.hmall.outdoor.common.Json.str(b, "orderId"),
      com.hmall.outdoor.common.Json.str(b, "kind")
    );
  }
}
