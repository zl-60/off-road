package com.hmall.outdoor.gateway;

import com.hmall.outdoor.common.*;
import java.nio.charset.StandardCharsets;
import org.springframework.cloud.gateway.filter.*;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class Security implements GlobalFilter, Ordered {

  private final Tokens tokens;

  public Security(Tokens tokens) {
    this.tokens = tokens;
  }

  public int getOrder() {
    return -100;
  }

  public Mono<Void> filter(ServerWebExchange e, GatewayFilterChain chain) {
    String p = e.getRequest().getPath().value();
    try {
      if (p.contains("/internal/")) throw new Problem(403, "内部接口禁止访问");
      boolean pub =
        p.equals("/api/auth/login") ||
        (e.getRequest().getMethodValue().equals("GET") && p.startsWith("/api/catalog"));
      if (!pub) {
        var claims = tokens.read(
          e.getRequest().getHeaders().getFirst("Authorization") == null
            ? ""
            : e.getRequest().getHeaders().getFirst("Authorization")
        );
        if (p.startsWith("/api/admin/") && !"ADMIN".equals(claims.get("role"))) throw new Problem(
          403,
          "需要管理员权限"
        );
      }
      return chain.filter(
        e
          .mutate()
          .request(b ->
            b.headers(h -> {
              h.remove("user-info");
              h.remove("X-Internal-Key");
            })
          )
          .build()
      );
    } catch (Problem x) {
      e.getResponse().setStatusCode(HttpStatus.valueOf(x.status));
      e.getResponse().getHeaders().set("Content-Type", "application/json;charset=UTF-8");
      return e.getResponse().writeWith(
        Mono.just(
          e
            .getResponse()
            .bufferFactory()
            .wrap(
              Json.write(java.util.Map.of("message", x.getMessage())).getBytes(
                StandardCharsets.UTF_8
              )
            )
        )
      );
    }
  }
}
