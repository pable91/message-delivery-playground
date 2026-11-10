package com.deliveryplayground.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);

    public void decreaseStock(Long orderId) {
        log.info("Decreasing stock for order {}", orderId);
    }
}
