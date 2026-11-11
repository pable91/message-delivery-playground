package com.deliveryplayground.order;

import com.deliveryplayground.outbox.OutboxEvent;
import com.deliveryplayground.outbox.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SerializationOrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public SerializationOrderService(OrderRepository orderRepository,
                                     OutboxRepository outboxRepository,
                                     ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Order createOrder(String itemName, int quantity, int totalPrice, Object eventPayload) {
        Order order = orderRepository.save(new Order(itemName, quantity, totalPrice));

        try {
            String payload = objectMapper.writeValueAsString(eventPayload);
            outboxRepository.save(new OutboxEvent("Order", order.getId(), "OrderCreated", payload));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Outbox 직렬화 실패", e);
        }

        return order;
    }
}
