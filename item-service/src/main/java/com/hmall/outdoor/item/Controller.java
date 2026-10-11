package com.hmall.outdoor.item;

import com.hmall.outdoor.common.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class Controller {

  private final Catalog c;

  public Controller(Catalog c) {
    this.c = c;
  }

  @GetMapping("/catalog/products")
  public Object list() {
    return c.list(false);
  }

  @GetMapping("/catalog/products/{id}")
  public Object detail(@PathVariable String id) {
    var p = c.detail(id);
    if (!"ACTIVE".equals(p.get("status"))) throw new Problem(404, "商品已下架");
    return p;
  }

  @GetMapping("/admin/products")
  public Object adminList() {
    return c.list(true);
  }

  @PostMapping("/admin/products")
  public Object save(@RequestBody Map<String, Object> b) {
    return c.save(b);
  }

  @PostMapping("/internal/reservations")
  public Object reserve(@RequestBody Map<String, Object> b) {
    return c.reserve(b);
  }

  @PostMapping("/internal/reservations/transition")
  public Object transition(@RequestBody Map<String, Object> b) {
    return c.transition(b);
  }
}
