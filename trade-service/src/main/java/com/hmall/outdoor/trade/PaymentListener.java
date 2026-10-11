package com.hmall.outdoor.trade;

import com.hmall.outdoor.common.Json;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

@Configuration
@ConditionalOnProperty(name = "outdoor.events.transport", havingValue = "rabbit")
public class PaymentListener {

  private final Orders orders;

  public PaymentListener(Orders orders) {
    this.orders = orders;
  }

  @Bean
  DirectExchange paymentExchange() {
    return new DirectExchange("shanye.payment");
  }

  @Bean
  DirectExchange paymentDeadExchange() {
    return new DirectExchange("shanye.payment.dead");
  }

  @Bean
  Queue paymentQueue() {
    return QueueBuilder.durable("shanye.payment-events")
      .deadLetterExchange("shanye.payment.dead")
      .deadLetterRoutingKey("dead")
      .build();
  }

  @Bean
  Queue paymentDeadQueue() {
    return new Queue("shanye.payment-events.dead", true);
  }

  @Bean
  Binding paymentBinding() {
    return BindingBuilder.bind(paymentQueue()).to(paymentExchange()).with("order.payment");
  }

  @Bean
  Binding paymentDeadBinding() {
    return BindingBuilder.bind(paymentDeadQueue()).to(paymentDeadExchange()).with("dead");
  }

  @RabbitListener(queues = "shanye.payment-events")
  public void receive(String body) {
    var b = Json.map(body);
    orders.complete(Json.str(b, "orderId"), Json.str(b, "kind"));
  }
}
