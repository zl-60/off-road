package com.hmall.outdoor.common;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
public class Client {

  private final RestTemplate http;
  private final String key;

  public Client(@Value("${outdoor.internal-key:shanye-local-demo-service-key}") String key) {
    this.key = key;
    SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
    f.setConnectTimeout(2000);
    f.setReadTimeout(8000);
    http = new RestTemplate(f);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> post(String url, Object body) {
    try {
      HttpHeaders h = new HttpHeaders();
      h.setContentType(MediaType.APPLICATION_JSON);
      h.set("X-Internal-Key", key);
      return http.postForObject(url, new HttpEntity<>(body, h), Map.class);
    } catch (HttpStatusCodeException e) {
      String msg = "远程操作失败";
      try {
        msg = Json.str(Json.map(e.getResponseBodyAsString()), "message");
      } catch (Exception ignored) {}
      throw new Problem(e.getRawStatusCode(), msg);
    } catch (ResourceAccessException e) {
      throw new Problem(503, "服务暂时不可用，操作将自动重试");
    }
  }
}
