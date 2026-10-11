package com.hmall.outdoor.pay;

import com.hmall.outdoor.common.Json;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "outdoor.events.transport", havingValue = "rabbit")
public class RabbitNotification implements PaymentNotification {

  private final RabbitTemplate rabbit;

  public RabbitNotification(RabbitTemplate rabbit) {
    this.rabbit = rabbit;
  }

  public void deliver(String id, String kind) {
    try {
      CorrelationData correlation = new CorrelationData(Json.id());
      rabbit.convertAndSend(
        "shanye.payment",
        "order.payment",
        Json.write(Map.of("orderId", id, "kind", kind)),
        correlation
      );
      if (
        !correlation.getFuture().get(5, TimeUnit.SECONDS).isAck() ||
        correlation.getReturned() != null
      ) throw new IllegalStateException("Payment event not routed");
    } catch (Exception e) {
      throw new IllegalStateException("Payment event awaiting retry", e);
    }
  }
}
