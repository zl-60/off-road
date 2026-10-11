package com.hmall.outdoor.user;

import com.hmall.outdoor.common.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class Controller {

  private final Accounts a;
  private final Db db;

  public Controller(Accounts a, Db db) {
    this.a = a;
    this.db = db;
  }

  @PostMapping("/auth/login")
  public Object login(@RequestBody Map<String, Object> b) {
    return a.login(b);
  }

  @GetMapping("/account")
  public Object me() {
    return a.account();
  }

  @GetMapping("/account/ledger")
  public Object ledger() {
    return db.rows(
      "SELECT * FROM wallet_ledger WHERE user_id=? ORDER BY created_at DESC",
      Session.user()
    );
  }

  @GetMapping("/addresses")
  public Object addresses() {
    return a.addresses();
  }

  @PostMapping("/addresses")
  public Object address(@RequestBody Map<String, Object> b) {
    return a.saveAddress(b);
  }

  @PostMapping("/internal/wallet")
  public Object wallet(@RequestBody Map<String, Object> b) {
    return a.wallet(b);
  }

  @PostMapping("/internal/address")
  public Object addressInternal(@RequestBody Map<String, Object> b) {
    return a.getAddress(b);
  }
}
