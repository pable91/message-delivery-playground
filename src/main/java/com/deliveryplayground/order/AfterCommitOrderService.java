package com.deliveryplayground.order;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AfterCommitOrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AfterCommitOrderService(OrderRepository orderRepository,
                                   ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Order createOrder(String itemName, int quantity, int totalPrice) {
        Order order = orderRepository.save(new Order(itemName, quantity, totalPrice));
        eventPublisher.publishEvent(new OrderCreatedEvent(order.getId()));
        return order;
    }
}
