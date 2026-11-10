package com.deliveryplayground.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    public void sendOrderConfirmation(Long orderId) {
        log.info("Sending order confirmation for order {}", orderId);
    }
}
