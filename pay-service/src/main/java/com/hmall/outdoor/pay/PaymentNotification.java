package com.hmall.outdoor.pay;

public interface PaymentNotification {
  void deliver(String orderId, String kind);
}
