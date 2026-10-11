package com.hmall.outdoor.pay;

import com.hmall.outdoor.common.Client;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
  name = "outdoor.events.transport",
  havingValue = "http",
  matchIfMissing = true
)
public class HttpNotification implements PaymentNotification {

  private final Client client;
  private final String trade;

  public HttpNotification(Client c, @Value("${TRADE_URL:http://127.0.0.1:8084}") String trade) {
    this.client = c;
    this.trade = trade;
  }

  public void deliver(String id, String kind) {
    client.post(trade + "/internal/payment-events", Map.of("orderId", id, "kind", kind));
  }
}
