package com.hmall.outdoor.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

public final class Json {

  public static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

  public static String write(Object x) {
    try {
      return MAPPER.writeValueAsString(x);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> map(String x) {
    try {
      return MAPPER.readValue(x, Map.class);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public static List<Map<String, Object>> list(String x) {
    try {
      return MAPPER.readValue(x, List.class);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static String str(Map<String, Object> m, String k) {
    Object x = m.get(k);
    return x == null ? "" : x.toString();
  }

  public static int num(Map<String, Object> m, String k) {
    try {
      return Math.toIntExact(Long.parseLong(str(m, k)));
    } catch (Exception e) {
      throw new Problem(400, "数量或金额格式不正确");
    }
  }

  public static String id() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
