package com.deliveryplayground.order;

import com.deliveryplayground.external.InventoryClient;
import com.deliveryplayground.external.NotificationClient;
import com.deliveryplayground.external.PaymentClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final NotificationClient notificationClient;

    public OrderService(OrderRepository orderRepository,
                        InventoryClient inventoryClient,
                        PaymentClient paymentClient,
                        NotificationClient notificationClient) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
        this.notificationClient = notificationClient;
    }

    @Transactional
    public Order createOrder(String itemName, int quantity, int totalPrice) {
        Order order = orderRepository.save(new Order(itemName, quantity, totalPrice));
        inventoryClient.decreaseStock(order.getId());
        paymentClient.processPayment(order.getId());
        notificationClient.sendOrderConfirmation(order.getId());
        return order;
    }
}
