package com.hmall.outdoor.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Tokens {

  private final byte[] key;

  public Tokens(
    @Value("${outdoor.token-secret:shanye-local-demo-token-key-change-for-deployment}") String key
  ) {
    this.key = key.getBytes(StandardCharsets.UTF_8);
  }

  private static String b64(byte[] x) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(x);
  }

  private byte[] sign(String x) {
    try {
      Mac m = Mac.getInstance("HmacSHA256");
      m.init(new SecretKeySpec(key, "HmacSHA256"));
      return m.doFinal(x.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public String issue(String id, String role) {
    String h = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    String p = b64(
      Json.write(
        Map.of("sub", id, "role", role, "exp", System.currentTimeMillis() / 1000 + 8 * 3600)
      ).getBytes(StandardCharsets.UTF_8)
    );
    return h + "." + p + "." + b64(sign(h + "." + p));
  }

  public Map<String, Object> read(String token) {
    try {
      String[] p = token.replaceFirst("^Bearer ", "").split("\\.");
      if (
        p.length != 3 ||
        !MessageDigest.isEqual(sign(p[0] + "." + p[1]), Base64.getUrlDecoder().decode(p[2]))
      ) throw new Exception();
      Map<String, Object> m = Json.map(
        new String(Base64.getUrlDecoder().decode(p[1]), StandardCharsets.UTF_8)
      );
      if (
        ((Number) m.get("exp")).longValue() <= System.currentTimeMillis() / 1000
      ) throw new Exception();
      if (
        !Set.of("USER", "ADMIN").contains(m.get("role")) || Json.str(m, "sub").isBlank()
      ) throw new Exception();
      return m;
    } catch (Exception e) {
      throw new Problem(401, "请登录或重新登录");
    }
  }
}
