package com.hmall.outdoor.trade;

import com.hmall.outdoor.common.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class Orders {

  private final Db db;
  private final Client client;
  private final String item, pay, user;
  private final int ttl;

  public Orders(
    Db db,
    Client client,
    @Value("${ITEM_URL:http://127.0.0.1:8082}") String item,
    @Value("${PAY_URL:http://127.0.0.1:8085}") String pay,
    @Value("${USER_URL:http://127.0.0.1:8081}") String user,
    @Value("${ORDER_TTL_SECONDS:900}") int ttl
  ) {
    this.db = db;
    this.client = client;
    this.item = item;
    this.pay = pay;
    this.user = user;
    this.ttl = ttl;
  }

  @SuppressWarnings("unchecked")
  public Object create(Map<String, Object> b) {
    String uid = Session.user(),
      key = Json.str(b, "requestKey");
    List<Map<String, Object>> lines = (List<Map<String, Object>>) b.get("lines");
    Problem.require(
      !key.isBlank() &&
        key.length() <= 100 &&
        lines != null &&
        !lines.isEmpty() &&
        lines.size() <= 50,
      "请选择装备并提供下单请求编号"
    );
    for (var line : lines)
      Problem.require(
        !Json.str(line, "productId").isBlank() &&
          Json.num(line, "quantity") > 0 &&
          Json.num(line, "quantity") <= 99,
        "购买数量无效"
      );
    String request = Json.write(Map.of("lines", lines, "addressId", Json.str(b, "addressId")));
    var old = db.rows("SELECT * FROM orders WHERE user_id=? AND request_key=?", uid, key);
    String id;
    if (!old.isEmpty()) {
      Problem.require(
        request.equals(old.get(0).get("requestJson")),
        "相同请求编号不能提交不同订单"
      );
      id = Json.str(old.get(0), "id");
    } else {
      var address = client.post(
        user + "/internal/address",
        Map.of("id", Json.str(b, "addressId"), "userId", uid)
      );
      id = Json.id();
      try {
        db.update(
          "INSERT INTO orders(id,user_id,request_key,request_json,address_json,status,expires_at) VALUES(?,?,?,?,?,?,?)",
          id,
          uid,
          key,
          request,
          Json.write(address),
          "CREATING",
          LocalDateTime.now().plusSeconds(ttl)
        );
        event(id, "CREATING", "提交订单");
      } catch (DuplicateKeyException e) {
        var existing = db.one("SELECT * FROM orders WHERE user_id=? AND request_key=?", uid, key);
        Problem.require(
          request.equals(existing.get("requestJson")),
          "相同请求编号不能提交不同订单"
        );
        id = Json.str(existing, "id");
      }
    }
    perform(id, true);
    return detail(id, false);
  }

  public Object command(String id, String command, Map<String, Object> b) {
    db.tx(() -> {
      var o = owned(id, true);
      String s = Json.str(o, "status");
      if (!command.equals("ship") && !Session.user().equals(o.get("userId"))) throw new Problem(
        404,
        "订单不存在"
      );
      switch (command) {
        case "pay":
          if (Set.of("PAID", "SHIPPED", "COMPLETED", "PAYING").contains(s)) break;
          Problem.require(s.equals("WAITING"), "当前订单不能支付");
          if (expired(o)) throw new Problem(409, "订单支付时间已过，请刷新订单");
          change(id, "PAYING", "正在处理演示余额支付");
          break;
        case "cancel":
          if (Set.of("CANCELLED", "CANCELLING").contains(s)) break;
          Problem.require(s.equals("WAITING"), "只有待支付订单可以取消");
          change(id, "CANCELLING", "用户取消订单");
          break;
        case "refund":
          if (Set.of("REFUNDED", "REFUNDING").contains(s)) break;
          Problem.require(s.equals("PAID"), "仅支持已支付且未发货订单整单退款");
          change(id, "REFUNDING", "申请自动整单退款");
          break;
        case "ship":
          Session.requireAdmin();
          Problem.require(s.equals("PAID"), "只有已支付且未退款订单可以发货");
          String carrier = Json.str(b, "carrier").trim(),
            tracking = Json.str(b, "trackingNo").trim();
          Problem.require(
            !carrier.isBlank() &&
              carrier.length() <= 64 &&
              !tracking.isBlank() &&
              tracking.length() <= 100,
            "请填写快递公司和单号"
          );
          db.update("UPDATE orders SET carrier=?,tracking_no=? WHERE id=?", carrier, tracking, id);
          change(id, "SHIPPED", "已发货：" + carrier + " " + tracking);
          break;
        case "receive":
          Problem.require(s.equals("SHIPPED"), "只有已发货订单可以确认收货");
          change(id, "COMPLETED", "用户确认收货");
          break;
        default:
          throw new Problem(400, "无效订单操作");
      }
      return null;
    });
    perform(id, true);
    return detail(id, false);
  }

  private Map<String, Object> owned(String id, boolean lock) {
    var o = db.one("SELECT * FROM orders WHERE id=?" + (lock ? " FOR UPDATE" : ""), id);
    if (!Session.admin() && !Session.user().equals(o.get("userId"))) throw new Problem(
      404,
      "订单不存在"
    );
    return o;
  }

  private boolean expired(Map<String, Object> o) {
    return !LocalDateTime.parse(Json.str(o, "expiresAt")).isAfter(LocalDateTime.now());
  }

  private void event(String id, String kind, String detail) {
    db.update(
      "INSERT INTO order_events(id,order_id,kind,detail) VALUES(?,?,?,?)",
      Json.id(),
      id,
      kind,
      detail
    );
  }

  private void change(String id, String status, String detail) {
    db.update(
      "UPDATE orders SET status=?,updated_at=CURRENT_TIMESTAMP,last_error=NULL WHERE id=?",
      status,
      id
    );
    event(id, status, detail);
  }

  public Object detail(String id, boolean internal) {
    Map<String, Object> o = internal
      ? db.one("SELECT * FROM orders WHERE id=?", id)
      : owned(id, false);
    o.remove("requestJson");
    String q = Json.str(o, "quoteJson");
    o.remove("quoteJson");
    o.put("quote", q.isBlank() ? Map.of() : Json.map(q));
    o.put("address", Json.map(Json.str(o, "addressJson")));
    o.remove("addressJson");
    o.put(
      "events",
      db.rows(
        "SELECT kind,detail,created_at FROM order_events WHERE order_id=? ORDER BY created_at,id",
        id
      )
    );
    return o;
  }

  public Object list(boolean admin) {
    var rows = admin
      ? db.rows("SELECT id FROM orders ORDER BY created_at DESC LIMIT 200")
      : db.rows(
          "SELECT id FROM orders WHERE user_id=? ORDER BY created_at DESC LIMIT 100",
          Session.user()
        );
    List<Object> r = new ArrayList<>();
    for (var row : rows) r.add(detail(Json.str(row, "id"), admin));
    return r;
  }

  public void perform(String id, boolean report) {
    var o = db.one("SELECT * FROM orders WHERE id=?", id);
    String s = Json.str(o, "status");
    try {
      switch (s) {
        case "CREATING":
          var request = Json.map(Json.str(o, "requestJson"));
          var quote = client.post(
            item + "/internal/reservations",
            Map.of("orderId", id, "lines", request.get("lines"))
          );
          db.tx(() -> {
            var current = db.one("SELECT status FROM orders WHERE id=? FOR UPDATE", id);
            if ("CREATING".equals(current.get("status"))) {
              db.update(
                "UPDATE orders SET quote_json=?,total=? WHERE id=?",
                Json.write(quote),
                quote.get("total"),
                id
              );
              change(id, "WAITING", "库存已预留，15分钟内完成支付");
            }
            return null;
          });
          break;
        case "PAYING":
          client.post(
            pay + "/internal/charge",
            Map.of("orderId", id, "userId", o.get("userId"), "amount", o.get("total"))
          );
          complete(id, "PAID");
          break;
        case "CANCELLING":
          client.post(
            item + "/internal/reservations/transition",
            Map.of("orderId", id, "action", "RELEASE")
          );
          db.tx(() -> {
            if (
              "CANCELLING".equals(
                db.one("SELECT status FROM orders WHERE id=? FOR UPDATE", id).get("status")
              )
            ) change(id, "CANCELLED", "订单关闭，库存已释放");
            return null;
          });
          break;
        case "REFUNDING":
          client.post(pay + "/internal/refund", Map.of("orderId", id));
          complete(id, "REFUNDED");
          break;
        default:
          break;
      }
    } catch (Problem e) {
      db.tx(() -> {
        var now = db.one("SELECT status FROM orders WHERE id=? FOR UPDATE", id);
        if (s.equals(now.get("status"))) {
          if (s.equals("CREATING") && e.status >= 400 && e.status < 500) change(
            id,
            "REJECTED",
            e.getMessage()
          );
          else if (s.equals("PAYING") && e.status == 409) change(id, "WAITING", e.getMessage());
          db.update("UPDATE orders SET last_error=? WHERE id=?", e.getMessage(), id);
        }
        return null;
      });
      if (report) throw e;
    } catch (Exception e) {
      db.update("UPDATE orders SET last_error=? WHERE id=?", "操作等待重试", id);
      if (report) throw new Problem(503, "操作处理中，请稍后刷新订单");
    }
  }

  public Object complete(String id, String kind) {
    var o = db.one("SELECT * FROM orders WHERE id=?", id);
    String source = kind.equals("PAID") ? "PAYING" : "REFUNDING";
    Problem.require(Set.of("PAID", "REFUNDED").contains(kind), "未知支付事件");
    if (!source.equals(o.get("status"))) return Map.of("ack", true);
    client.post(
      item + "/internal/reservations/transition",
      Map.of("orderId", id, "action", kind.equals("PAID") ? "COMMIT" : "REFUND")
    );
    db.tx(() -> {
      if (
        source.equals(db.one("SELECT status FROM orders WHERE id=? FOR UPDATE", id).get("status"))
      ) change(id, kind, kind.equals("PAID") ? "演示余额支付成功" : "已退回演示余额，库存已恢复");
      return null;
    });
    return Map.of("ack", true);
  }

  @Scheduled(fixedDelayString = "${ORDER_SWEEP_MS:3000}")
  public void sweep() {
    for (var row : db.rows(
      "SELECT id FROM orders WHERE status='WAITING' AND expires_at<=CURRENT_TIMESTAMP LIMIT 100"
    )) {
      String id = Json.str(row, "id");
      db.tx(() -> {
        var o = db.one("SELECT * FROM orders WHERE id=? FOR UPDATE", id);
        if ("WAITING".equals(o.get("status")) && expired(o)) change(
          id,
          "CANCELLING",
          "支付超时，自动关闭"
        );
        return null;
      });
    }
    for (var row : db.rows(
      "SELECT id FROM orders WHERE status IN ('CREATING','PAYING','CANCELLING','REFUNDING') ORDER BY created_at LIMIT 100"
    ))
      perform(Json.str(row, "id"), false);
  }

  public Object dashboard() {
    var all = db.rows("SELECT status,total,created_at FROM orders");
    long revenue = 0;
    int paid = 0,
      pending = 0,
      shipped = 0,
      refunds = 0;
    TreeMap<String, long[]> days = new TreeMap<>();
    for (int i = 6; i >= 0; i--) days.put(LocalDate.now().minusDays(i).toString(), new long[2]);
    for (var o : all) {
      String s = Json.str(o, "status");
      if (Set.of("PAID", "SHIPPED", "COMPLETED").contains(s)) {
        paid++;
        revenue += Json.num(o, "total");
        String day = Json.str(o, "createdAt").substring(0, 10);
        if (days.containsKey(day)) {
          days.get(day)[0]++;
          days.get(day)[1] += Json.num(o, "total");
        }
      }
      if (s.equals("WAITING")) pending++;
      if (s.equals("PAID")) shipped++;
      if (s.equals("REFUNDED")) refunds++;
    }
    List<Object> trend = new ArrayList<>();
    days.forEach((d, v) -> trend.add(Map.of("date", d, "orders", v[0], "revenue", v[1])));
    return Map.of(
      "orderCount",
      all.size(),
      "paidOrders",
      paid,
      "revenue",
      revenue,
      "pendingPayment",
      pending,
      "pendingShipping",
      shipped,
      "refunds",
      refunds,
      "trend",
      trend
    );
  }
}
