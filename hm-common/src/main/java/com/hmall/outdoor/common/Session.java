package com.hmall.outdoor.common;

import java.util.Map;

public final class Session {

  private static final ThreadLocal<Map<String, Object>> VALUE = new ThreadLocal<>();

  public static void set(Map<String, Object> v) {
    VALUE.set(v);
  }

  public static void clear() {
    VALUE.remove();
  }

  public static String user() {
    if (VALUE.get() == null) throw new Problem(401, "请先登录");
    return Json.str(VALUE.get(), "sub");
  }

  public static boolean admin() {
    return VALUE.get() != null && "ADMIN".equals(VALUE.get().get("role"));
  }

  public static void requireAdmin() {
    if (!admin()) throw new Problem(403, "需要管理员权限");
  }
}
