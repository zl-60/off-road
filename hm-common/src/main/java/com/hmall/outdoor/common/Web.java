package com.hmall.outdoor.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import javax.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class Web implements WebMvcConfigurer {

  private final Tokens tokens;
  private final String internal;

  public Web(
    Tokens tokens,
    @Value("${outdoor.internal-key:shanye-local-demo-service-key}") String internal
  ) {
    this.tokens = tokens;
    this.internal = internal;
  }

  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(
      new HandlerInterceptor() {
        public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
          Session.clear();
          String p = req.getRequestURI();
          if (
            p.equals("/health") ||
            p.equals("/auth/login") ||
            (req.getMethod().equals("GET") && p.startsWith("/catalog"))
          ) return true;
          if (p.startsWith("/internal/")) {
            String k = req.getHeader("X-Internal-Key");
            if (
              k == null ||
              !MessageDigest.isEqual(
                k.getBytes(StandardCharsets.UTF_8),
                internal.getBytes(StandardCharsets.UTF_8)
              )
            ) throw new Problem(403, "内部接口禁止访问");
            return true;
          }
          Session.set(tokens.read(Optional.ofNullable(req.getHeader("Authorization")).orElse("")));
          if (p.startsWith("/admin/")) Session.requireAdmin();
          return true;
        }

        public void afterCompletion(
          HttpServletRequest req,
          HttpServletResponse res,
          Object h,
          Exception e
        ) {
          Session.clear();
        }
      }
    );
  }

  @RestControllerAdvice
  public static class Errors {

    @ExceptionHandler(Problem.class)
    public ResponseEntity<?> problem(Problem p) {
      return ResponseEntity.status(p.status).body(Map.of("message", p.getMessage()));
    }

    @ExceptionHandler({
      org.springframework.http.converter.HttpMessageNotReadableException.class,
      org.springframework.web.bind.MissingServletRequestParameterException.class,
      IllegalArgumentException.class,
      ArithmeticException.class,
    })
    public ResponseEntity<?> bad(Exception p) {
      return ResponseEntity.badRequest().body(Map.of("message", "请求格式不正确"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> failure(Exception e) {
      org.slf4j.LoggerFactory.getLogger(Errors.class).error("Request failed", e);
      return ResponseEntity.status(500).body(Map.of("message", "服务暂时无法完成请求，请稍后重试"));
    }
  }
}
