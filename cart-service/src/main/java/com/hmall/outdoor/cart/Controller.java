package com.hmall.outdoor.cart;

import com.hmall.outdoor.common.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class Controller {

  private final Db db;

  public Controller(Db db) {
    this.db = db;
  }

  @GetMapping("/cart")
  public Object list() {
    return db.rows(
      "SELECT product_id,quantity FROM cart_lines WHERE user_id=? ORDER BY product_id",
      Session.user()
    );
  }

  @PostMapping("/cart")
  public Object save(@RequestBody Map<String, Object> b) {
    String id = Json.str(b, "productId");
    int q = Json.num(b, "quantity");
    Problem.require(
      !id.isBlank() && id.length() <= 64 && q >= 0 && q <= 99,
      "购物车数量必须为0到99"
    );
    db.tx(() -> {
      db.update("DELETE FROM cart_lines WHERE user_id=? AND product_id=?", Session.user(), id);
      if (q > 0) db.update("INSERT INTO cart_lines VALUES(?,?,?)", Session.user(), id, q);
      return null;
    });
    return list();
  }

  @DeleteMapping("/cart")
  public Object clear() {
    db.update("DELETE FROM cart_lines WHERE user_id=?", Session.user());
    return Map.of("cleared", true);
  }
}
