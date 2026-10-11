package com.hmall.outdoor.item;

import com.hmall.outdoor.common.*;
import java.util.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class Catalog {

  private final Db db;

  public Catalog(Db db) {
    this.db = db;
  }

  @Bean
  ApplicationRunner seed() {
    return args ->
      db.tx(() -> {
        if (!db.exists("SELECT id FROM products")) {
          seedItem(
            "ridge-tent",
            "山脊 · 双人轻量帐篷",
            89900,
            24,
            "帐篷",
            "露营",
            "双层通风结构，陪你在山野安稳入睡。",
            "{\"人数\":\"2人\",\"重量\":\"2.4kg\",\"季节\":\"三季\"}",
            "tent"
          );
          seedItem(
            "fold-chair",
            "溪岸 · 折叠露营椅",
            23900,
            48,
            "桌椅",
            "露营",
            "收起城市的忙碌，坐下来听风。",
            "{\"承重\":\"120kg\",\"重量\":\"1.2kg\"}",
            "chair"
          );
          seedItem(
            "warm-lamp",
            "微光 · 营地氛围灯",
            16900,
            40,
            "照明",
            "露营",
            "暖光与续航兼得，照亮每一个营地夜晚。",
            "{\"续航\":\"18小时\",\"防护\":\"IPX4\"}",
            "lamp"
          );
          seedItem(
            "trail-pack",
            "远行 · 轻量徒步背包",
            45900,
            32,
            "背包",
            "徒步",
            "透气背负与分区收纳，让路途更轻盈。",
            "{\"容量\":\"35L\",\"重量\":\"850g\"}",
            "pack"
          );
          seedItem(
            "trek-poles",
            "林径 · 碳纤维登山杖",
            32900,
            30,
            "登山装备",
            "徒步",
            "一对可靠的支点，陪你走过起伏山路。",
            "{\"材质\":\"碳纤维\",\"数量\":\"一对\"}",
            "poles"
          );
          seedItem(
            "field-flask",
            "原野 · 真空保温水壶",
            12900,
            50,
            "水具",
            "徒步",
            "从清晨到日落，留住一口恰好的温度。",
            "{\"容量\":\"750ml\",\"材质\":\"304不锈钢\"}",
            "flask"
          );
          seedItem(
            "down-bag",
            "夜栖 · 三季睡袋",
            59900,
            18,
            "睡眠",
            "露营",
            "柔软包裹与轻量收纳，让野外也有好梦。",
            "{\"舒适温度\":\"5°C\",\"重量\":\"950g\"}",
            "sleep"
          );
          seedItem(
            "camp-table",
            "松间 · 蛋卷露营桌",
            36900,
            20,
            "桌椅",
            "露营",
            "一张桌子，盛下营地里的所有小确幸。",
            "{\"尺寸\":\"90×60cm\",\"材质\":\"铝合金\"}",
            "table"
          );
          db.update(
            "INSERT INTO products(id,name,type,price,stock,category,scene,brand,description,spec,image,status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
            "camp-duo",
            "双人营地 · 入门套装",
            "BUNDLE",
            139900,
            0,
            "装备套装",
            "露营",
            "山野装备",
            "一顶帐篷、两把椅子、一盏营灯。把第一次露营，交给一套完整的准备。",
            "{}",
            "/assets/kit.svg",
            "ACTIVE"
          );
          db.update("INSERT INTO bundle_parts VALUES(?,?,?)", "camp-duo", "ridge-tent", 1);
          db.update("INSERT INTO bundle_parts VALUES(?,?,?)", "camp-duo", "fold-chair", 2);
          db.update("INSERT INTO bundle_parts VALUES(?,?,?)", "camp-duo", "warm-lamp", 1);
          db.update(
            "INSERT INTO products(id,name,type,price,stock,category,scene,brand,description,spec,image,status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
            "trail-kit",
            "轻装上路 · 徒步套装",
            "BUNDLE",
            79900,
            0,
            "装备套装",
            "徒步",
            "山野装备",
            "背包、登山杖与水壶，为一日山行准备好。",
            "{}",
            "/assets/trail.svg",
            "ACTIVE"
          );
          for (String id : List.of("trail-pack", "trek-poles", "field-flask"))
            db.update("INSERT INTO bundle_parts VALUES(?,?,?)", "trail-kit", id, 1);
        }
        return null;
      });
  }

  private void seedItem(
    String id,
    String name,
    int price,
    int stock,
    String cat,
    String scene,
    String desc,
    String spec,
    String image
  ) {
    db.update(
      "INSERT INTO products(id,name,type,price,stock,category,scene,brand,description,spec,image,status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
      id,
      name,
      "SINGLE",
      price,
      stock,
      cat,
      scene,
      "山野装备",
      desc,
      spec,
      "/assets/" + image + ".svg",
      "ACTIVE"
    );
  }

  public List<Map<String, Object>> list(boolean admin) {
    List<Map<String, Object>> r = db.rows(
      admin
        ? "SELECT * FROM products ORDER BY created_at,id"
        : "SELECT * FROM products WHERE status='ACTIVE' ORDER BY created_at,id"
    );
    r.forEach(this::decorate);
    return r;
  }

  public Map<String, Object> detail(String id) {
    Map<String, Object> p = db.one("SELECT * FROM products WHERE id=?", id);
    decorate(p);
    return p;
  }

  private void decorate(Map<String, Object> p) {
    p.put("spec", Json.map(Json.str(p, "spec")));
    if ("BUNDLE".equals(p.get("type"))) {
      var parts = db.rows(
        "SELECT p.id,p.name,p.price,p.stock,p.image,p.status,b.quantity FROM bundle_parts b JOIN products p ON p.id=b.item_id WHERE b.bundle_id=? ORDER BY p.id",
        p.get("id")
      );
      int stock = Integer.MAX_VALUE,
        original = 0;
      for (var b : parts) {
        int qty = ((Number) b.get("quantity")).intValue();
        stock = Math.min(
          stock,
          "ACTIVE".equals(b.get("status")) ? ((Number) b.get("stock")).intValue() / qty : 0
        );
        original = Math.addExact(
          original,
          Math.multiplyExact(((Number) b.get("price")).intValue(), qty)
        );
      }
      p.put("stock", parts.isEmpty() ? 0 : stock);
      p.put("originalPrice", original);
      p.put("parts", parts);
    }
  }

  public Map<String, Object> reserve(Map<String, Object> b) {
    try {
      return reserveOnce(b);
    } catch (org.springframework.dao.DuplicateKeyException e) {
      return reserveOnce(b);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> reserveOnce(Map<String, Object> b) {
    String order = Json.str(b, "orderId");
    List<Map<String, Object>> lines = (List<Map<String, Object>>) b.get("lines");
    Problem.require(
      !order.isBlank() && lines != null && !lines.isEmpty() && lines.size() <= 50,
      "请选择要购买的装备"
    );
    return db.tx(() -> {
      List<Map<String, Object>> old = db.rows(
        "SELECT * FROM reservations WHERE order_id=? FOR UPDATE",
        order
      );
      String request = Json.write(lines);
      if (!old.isEmpty()) {
        Problem.require(request.equals(old.get(0).get("requestJson")), "订单库存请求冲突");
        Problem.require(
          !Set.of("RELEASED", "REFUNDED").contains(old.get(0).get("status")),
          "该订单库存已释放"
        );
        return Json.map(Json.str(old.get(0), "quoteJson"));
      }
      TreeMap<String, Integer> buy = new TreeMap<>();
      for (var line : lines) {
        String id = Json.str(line, "productId");
        int qty = Json.num(line, "quantity");
        Problem.require(qty > 0 && qty <= 99, "购买数量必须为1到99");
        buy.merge(id, qty, Math::addExact);
      }
      Map<String, Map<String, Object>> products = new TreeMap<>();
      for (String id : buy.keySet()) {
        var product = db.one("SELECT * FROM products WHERE id=?", id);
        if ("BUNDLE".equals(product.get("type"))) product = db.one(
          "SELECT * FROM products WHERE id=? FOR UPDATE",
          id
        );
        products.put(id, product);
      }
      TreeMap<String, Integer> demand = new TreeMap<>();
      List<Map<String, Object>> snapshots = new ArrayList<>();
      int total = 0;
      for (var entry : buy.entrySet()) {
        var p = products.get(entry.getKey());
        Problem.require("ACTIVE".equals(p.get("status")), "商品已下架");
        int qty = entry.getValue();
        Problem.require(qty <= 99, "单件购买数量超过上限");
        List<Map<String, Object>> components = new ArrayList<>();
        if ("BUNDLE".equals(p.get("type"))) {
          var parts = db.rows(
            "SELECT item_id,quantity FROM bundle_parts WHERE bundle_id=? ORDER BY item_id",
            p.get("id")
          );
          Problem.require(!parts.isEmpty(), "套装尚未配置组件");
          for (var part : parts) {
            String id = Json.str(part, "itemId");
            int num = Json.num(part, "quantity");
            components.add(Map.of("itemId", id, "quantity", num));
            demand.merge(id, Math.multiplyExact(qty, num), Math::addExact);
          }
        } else {
          demand.merge(entry.getKey(), qty, Math::addExact);
          components.add(Map.of("itemId", entry.getKey(), "quantity", 1));
        }
        int price = Json.num(p, "price");
        total = Math.addExact(total, Math.multiplyExact(price, qty));
        snapshots.add(
          Map.of(
            "productId",
            p.get("id"),
            "name",
            p.get("name"),
            "type",
            p.get("type"),
            "price",
            price,
            "quantity",
            qty,
            "image",
            p.get("image"),
            "components",
            components
          )
        );
      }
      for (var entry : demand.entrySet()) {
        var p = db.one("SELECT * FROM products WHERE id=? FOR UPDATE", entry.getKey());
        Problem.require(
          "SINGLE".equals(p.get("type")) && "ACTIVE".equals(p.get("status")),
          "套装组件已下架"
        );
        if (
          db.update(
            "UPDATE products SET stock=stock-?,reserved=reserved+? WHERE id=? AND stock>=?",
            entry.getValue(),
            entry.getValue(),
            entry.getKey(),
            entry.getValue()
          ) != 1
        ) throw new Problem(409, p.get("name") + " 库存不足");
      }
      Map<String, Object> quote = Map.of("total", total, "lines", snapshots, "components", demand);
      db.update(
        "INSERT INTO reservations VALUES(?,?,?,?)",
        order,
        "RESERVED",
        request,
        Json.write(quote)
      );
      for (var e : demand.entrySet())
        db.update("INSERT INTO reservation_lines VALUES(?,?,?)", order, e.getKey(), e.getValue());
      return quote;
    });
  }

  public Map<String, Object> transition(Map<String, Object> b) {
    String order = Json.str(b, "orderId"),
      action = Json.str(b, "action");
    return db.tx(() -> {
      var r = db.one("SELECT * FROM reservations WHERE order_id=? FOR UPDATE", order);
      String status = Json.str(r, "status");
      String target;
      switch (action) {
        case "COMMIT":
          target = "COMMITTED";
          break;
        case "RELEASE":
          target = "RELEASED";
          break;
        case "REFUND":
          target = "REFUNDED";
          break;
        default:
          throw new Problem(400, "无效库存操作");
      }
      if (target.equals(status)) return Map.of("status", status);
      Problem.require(
        (action.equals("REFUND") && status.equals("COMMITTED")) ||
          (!action.equals("REFUND") && status.equals("RESERVED")),
        "库存状态不允许该操作"
      );
      for (var line : db.rows(
        "SELECT * FROM reservation_lines WHERE order_id=? ORDER BY item_id",
        order
      )) {
        int q = Json.num(line, "quantity");
        String id = Json.str(line, "itemId");
        if (action.equals("COMMIT")) db.update(
          "UPDATE products SET reserved=reserved-?,sold=sold+? WHERE id=?",
          q,
          q,
          id
        );
        else if (action.equals("RELEASE")) db.update(
          "UPDATE products SET stock=stock+?,reserved=reserved-? WHERE id=?",
          q,
          q,
          id
        );
        else db.update("UPDATE products SET stock=stock+?,sold=sold-? WHERE id=?", q, q, id);
      }
      db.update("UPDATE reservations SET status=? WHERE order_id=?", target, order);
      return Map.of("status", target);
    });
  }

  @SuppressWarnings("unchecked")
  public Object save(Map<String, Object> b) {
    Session.requireAdmin();
    String id = Json.str(b, "id"),
      name = Json.str(b, "name").trim(),
      type = Json.str(b, "type"),
      status = Json.str(b, "status");
    int price = Json.num(b, "price"),
      stock = Json.num(b, "stock");
    String image = Json.str(b, "image");
    Problem.require(
      name.length() > 0 &&
        name.length() <= 120 &&
        price > 0 &&
        price <= 100000000 &&
        stock >= 0 &&
        stock <= 1000000 &&
        Set.of("SINGLE", "BUNDLE").contains(type) &&
        Set.of("ACTIVE", "INACTIVE").contains(status),
      "商品名称、金额、库存或状态不正确"
    );
    Problem.require(
      image.startsWith("/assets/") || image.startsWith("https://"),
      "请使用本站图片或HTTPS图片地址"
    );
    String chosen = id.isBlank() ? Json.id() : id;
    return db.tx(() -> {
      if (db.exists("SELECT id FROM products WHERE id=?", chosen)) {
        var old = db.one("SELECT * FROM products WHERE id=? FOR UPDATE", chosen);
        Problem.require(type.equals(old.get("type")), "已有商品不能改变商品类型");
        db.update(
          "UPDATE products SET name=?,price=?,stock=?,category=?,scene=?,brand=?,description=?,spec=?,image=?,status=? WHERE id=?",
          name,
          price,
          stock,
          b.get("category"),
          b.get("scene"),
          b.get("brand"),
          b.get("description"),
          Json.write(b.getOrDefault("spec", Map.of())),
          image,
          status,
          chosen
        );
      } else db.update(
        "INSERT INTO products(id,name,type,price,stock,category,scene,brand,description,spec,image,status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
        chosen,
        name,
        type,
        price,
        stock,
        b.get("category"),
        b.get("scene"),
        b.get("brand"),
        b.get("description"),
        Json.write(b.getOrDefault("spec", Map.of())),
        image,
        status
      );
      if (type.equals("BUNDLE")) {
        List<Map<String, Object>> parts = (List<Map<String, Object>>) b.get("parts");
        Problem.require(
          parts != null && !parts.isEmpty() && parts.size() <= 20,
          "套装至少需要一个组件"
        );
        parts.sort(java.util.Comparator.comparing(p -> Json.str(p, "itemId")));
        Set<String> ids = new HashSet<>();
        for (var p : parts) {
          String pid = Json.str(p, "itemId");
          int q = Json.num(p, "quantity");
          Problem.require(ids.add(pid) && q > 0 && q <= 99, "套装组件重复或数量无效");
          var component = db.one("SELECT type FROM products WHERE id=? FOR UPDATE", pid);
          Problem.require("SINGLE".equals(component.get("type")), "套装组件必须是单品");
        }
        db.update("DELETE FROM bundle_parts WHERE bundle_id=?", chosen);
        for (var p : parts)
          db.update(
            "INSERT INTO bundle_parts VALUES(?,?,?)",
            chosen,
            p.get("itemId"),
            Json.num(p, "quantity")
          );
      }
      return detail(chosen);
    });
  }
}
