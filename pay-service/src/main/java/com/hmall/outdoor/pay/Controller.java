package com.hmall.outdoor.pay;

import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class Controller {

  private final Payments p;

  public Controller(Payments p) {
    this.p = p;
  }

  @PostMapping("/internal/charge")
  public Object charge(@RequestBody Map<String, Object> b) {
    return p.charge(b);
  }

  @PostMapping("/internal/refund")
  public Object refund(@RequestBody Map<String, Object> b) {
    return p.refund(b);
  }

  @GetMapping("/admin/payments")
  public Object list() {
    return p.list();
  }
}
