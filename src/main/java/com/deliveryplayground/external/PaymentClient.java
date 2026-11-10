package com.deliveryplayground.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);

    public void processPayment(Long orderId) {
        log.info("Processing payment for order {}", orderId);
    }
}
