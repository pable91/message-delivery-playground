package com.deliveryplayground.order;

import com.deliveryplayground.external.PaymentClient;
import com.deliveryplayground.outbox.OutboxEvent;
import com.deliveryplayground.outbox.OutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxOrderService {

    private final OrderRepository orderRepository;
    private final OutboxService outboxService;
    private final PaymentClient paymentClient;

    public OutboxOrderService(OrderRepository orderRepository,
                              OutboxService outboxService,
                              PaymentClient paymentClient) {
        this.orderRepository = orderRepository;
        this.outboxService = outboxService;
        this.paymentClient = paymentClient;
    }

    @Transactional
    public Order createOrder(String itemName, int quantity, int totalPrice) {
        Order order = orderRepository.save(new Order(itemName, quantity, totalPrice));

        outboxService.save(new OutboxEvent(
                "Order",
                order.getId(),
                "OrderCreated",
                "{\"orderId\":" + order.getId() + "}"
        ));

        paymentClient.processPayment(order.getId());

        return order;
    }
}
