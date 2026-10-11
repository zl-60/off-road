package com.hmall.outdoor.pay;

import com.hmall.outdoor.common.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class Payments {

  private final Db db;
  private final Client client;
  private final String user, trade;
  private final PaymentNotification notifications;

  public Payments(
    Db db,
    Client client,
    @Value("${USER_URL:http://127.0.0.1:8081}") String user,
    @Value("${TRADE_URL:http://127.0.0.1:8084}") String trade,
    PaymentNotification notifications
  ) {
    this.notifications = notifications;
    this.db = db;
    this.client = client;
    this.user = user;
    this.trade = trade;
  }

  public Map<String, Object> charge(Map<String, Object> b) {
    String id = Json.str(b, "orderId"),
      uid = Json.str(b, "userId");
    int amount = Json.num(b, "amount");
    Problem.require(!id.isBlank() && !uid.isBlank() && amount > 0, "支付参数无效");
    try {
      db.update(
        "INSERT INTO payments(order_id,user_id,amount,status) VALUES(?,?,?,?)",
        id,
        uid,
        amount,
        "WAITING"
      );
    } catch (DuplicateKeyException ignored) {}
    return db.tx(() -> {
      var p = db.one("SELECT * FROM payments WHERE order_id=? FOR UPDATE", id);
      Problem.require(
        uid.equals(p.get("userId")) && amount == Json.num(p, "amount"),
        "支付单参数不一致"
      );
      String status = Json.str(p, "status");
      if (status.equals("PAID")) return Map.of("status", "PAID");
      Problem.require(status.equals("WAITING"), "支付单已退款");
      client.post(
        user + "/internal/wallet",
        Map.of("operationId", "pay-" + id, "userId", uid, "amount", amount, "kind", "DEBIT")
      );
      db.update(
        "UPDATE payments SET status='PAID',updated_at=CURRENT_TIMESTAMP WHERE order_id=?",
        id
      );
      db.update(
        "INSERT INTO outbox(event_key,order_id,kind) VALUES(?,?,?)",
        "pay-" + id,
        id,
        "PAID"
      );
      return Map.of("status", "PAID");
    });
  }

  public Map<String, Object> refund(Map<String, Object> b) {
    String id = Json.str(b, "orderId");
    return db.tx(() -> {
      var p = db.one("SELECT * FROM payments WHERE order_id=? FOR UPDATE", id);
      if ("REFUNDED".equals(p.get("status"))) return Map.of("status", "REFUNDED");
      Problem.require("PAID".equals(p.get("status")), "该支付单不能退款");
      client.post(
        user + "/internal/wallet",
        Map.of(
          "operationId",
          "refund-" + id,
          "userId",
          p.get("userId"),
          "amount",
          p.get("amount"),
          "kind",
          "REFUND"
        )
      );
      db.update(
        "UPDATE payments SET status='REFUNDED',updated_at=CURRENT_TIMESTAMP WHERE order_id=?",
        id
      );
      db.update(
        "INSERT INTO outbox(event_key,order_id,kind) VALUES(?,?,?)",
        "refund-" + id,
        id,
        "REFUNDED"
      );
      return Map.of("status", "REFUNDED");
    });
  }

  @Scheduled(fixedDelayString = "${OUTBOX_INTERVAL_MS:3000}")
  public void publish() {
    for (var e : db.rows(
      "SELECT * FROM outbox WHERE delivered=0 AND next_at<=CURRENT_TIMESTAMP ORDER BY next_at LIMIT 30"
    )) {
      try {
        notifications.deliver(Json.str(e, "orderId"), Json.str(e, "kind"));
        db.update(
          "UPDATE outbox SET delivered=1,attempts=attempts+1 WHERE event_key=?",
          e.get("eventKey")
        );
      } catch (Exception x) {
        db.update(
          "UPDATE outbox SET attempts=attempts+1,next_at=? WHERE event_key=?",
          java.time.LocalDateTime.now().plusSeconds(5),
          e.get("eventKey")
        );
        org.slf4j.LoggerFactory.getLogger(Payments.class).warn(
          "Payment event pending: {}",
          e.get("eventKey")
        );
      }
    }
  }

  public Object list() {
    return Map.of(
      "payments",
      db.rows("SELECT * FROM payments ORDER BY created_at DESC"),
      "events",
      db.rows("SELECT * FROM outbox ORDER BY next_at DESC")
    );
  }
}
