package com.resala.payment;

import java.util.List;

public interface PaymentGateway {
  List<PaymentMethod> getAvailableMethods();
  GatewayResult charge(PaymentCommand command);
}

