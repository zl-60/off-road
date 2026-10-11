package com.hmall.outdoor.user;

import com.hmall.outdoor.common.*;
import java.util.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class Accounts {

  private final Db db;
  private final Tokens tokens;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  public Accounts(Db db, Tokens tokens) {
    this.db = db;
    this.tokens = tokens;
  }

  @Bean
  ApplicationRunner seed() {
    return args ->
      db.tx(() -> {
        if (!db.exists("SELECT id FROM accounts WHERE id='u-demo'")) {
          db.update(
            "INSERT INTO accounts(id,username,password,role,balance) VALUES(?,?,?,?,?)",
            "u-demo",
            "demo",
            encoder.encode("Shanye123!"),
            "USER",
            500000
          );
          db.update(
            "INSERT INTO accounts(id,username,password,role,balance) VALUES(?,?,?,?,?)",
            "u-admin",
            "admin",
            encoder.encode("ShanyeAdmin123!"),
            "ADMIN",
            0
          );
          db.update(
            "INSERT INTO accounts(id,username,password,role,balance) VALUES(?,?,?,?,?)",
            "u-low",
            "lowbalance",
            encoder.encode("Shanye123!"),
            "USER",
            100
          );
          db.update(
            "INSERT INTO addresses VALUES(?,?,?,?,?)",
            "addr-demo",
            "u-demo",
            "林野",
            "13800138000",
            "上海市徐汇区 山野路 18 号"
          );
        }
        return null;
      });
  }

  public Map<String, Object> login(Map<String, Object> body) {
    String username = Json.str(body, "username");
    List<Map<String, Object>> rows = db.rows("SELECT * FROM accounts WHERE username=?", username);
    if (
      rows.isEmpty() ||
      !encoder.matches(Json.str(body, "password"), Json.str(rows.get(0), "password"))
    ) throw new Problem(401, "账号或密码不正确");
    Map<String, Object> u = rows.get(0);
    u.remove("password");
    u.put("token", tokens.issue(Json.str(u, "id"), Json.str(u, "role")));
    return u;
  }

  public Map<String, Object> account() {
    return db.one("SELECT id,username,role,balance FROM accounts WHERE id=?", Session.user());
  }

  public Map<String, Object> wallet(Map<String, Object> b) {
    String op = Json.str(b, "operationId"),
      uid = Json.str(b, "userId"),
      kind = Json.str(b, "kind");
    int amount = Json.num(b, "amount");
    Problem.require(
      !op.isBlank() && amount > 0 && Set.of("DEBIT", "REFUND").contains(kind),
      "资金操作无效"
    );
    return db.tx(() -> {
      db.one("SELECT id FROM accounts WHERE id=? FOR UPDATE", uid);
      List<Map<String, Object>> previous = db.rows(
        "SELECT * FROM wallet_ledger WHERE operation_id=?",
        op
      );
      long delta = kind.equals("DEBIT") ? -((long) amount) : amount;
      if (!previous.isEmpty()) {
        Map<String, Object> p = previous.get(0);
        Problem.require(
          uid.equals(p.get("userId")) &&
            ((Number) p.get("delta")).longValue() == delta &&
            kind.equals(p.get("kind")),
          "资金操作编号冲突"
        );
        return Map.of("applied", true);
      }
      if (kind.equals("DEBIT")) {
        if (
          db.update(
            "UPDATE accounts SET balance=balance-? WHERE id=? AND balance>=?",
            amount,
            uid,
            amount
          ) != 1
        ) throw new Problem(409, "演示余额不足");
      } else db.update("UPDATE accounts SET balance=balance+? WHERE id=?", amount, uid);
      db.update(
        "INSERT INTO wallet_ledger(operation_id,user_id,delta,kind) VALUES(?,?,?,?)",
        op,
        uid,
        delta,
        kind
      );
      return Map.of("applied", true);
    });
  }

  public List<Map<String, Object>> addresses() {
    return db.rows("SELECT * FROM addresses WHERE user_id=?", Session.user());
  }

  public Map<String, Object> saveAddress(Map<String, Object> b) {
    String name = Json.str(b, "recipient").trim(),
      phone = Json.str(b, "phone").trim(),
      detail = Json.str(b, "detail").trim();
    Problem.require(
      name.length() > 0 &&
        name.length() <= 64 &&
        phone.matches("[0-9+ -]{7,24}") &&
        detail.length() > 0 &&
        detail.length() <= 500,
      "请填写完整的收货人、电话和地址"
    );
    String id = Json.id();
    db.update("INSERT INTO addresses VALUES(?,?,?,?,?)", id, Session.user(), name, phone, detail);
    return Map.of("id", id, "recipient", name, "phone", phone, "detail", detail);
  }

  public Map<String, Object> getAddress(Map<String, Object> b) {
    return db.one(
      "SELECT id,recipient,phone,detail FROM addresses WHERE id=? AND user_id=?",
      b.get("id"),
      b.get("userId")
    );
  }
}
