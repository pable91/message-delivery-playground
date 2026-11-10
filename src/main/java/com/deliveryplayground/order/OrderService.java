package com.deliveryplayground.order;

import com.deliveryplayground.external.NotificationClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final NotificationClient notificationClient;

    public OrderService(OrderRepository orderRepository,
                        NotificationClient notificationClient) {
        this.orderRepository = orderRepository;
        this.notificationClient = notificationClient;
    }

    @Transactional
    public Order createOrder(String itemName, int quantity, int totalPrice) {
        Order order = orderRepository.save(new Order(itemName, quantity, totalPrice));
        notificationClient.sendOrderConfirmation(order.getId());
        return order;
    }
}
