package com.deliveryplayground.order;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsyncOrderService {

    private static final Logger log = LoggerFactory.getLogger(AsyncOrderService.class);

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    public AsyncOrderService(OrderRepository orderRepository,
                             Optional<RabbitTemplate> rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.rabbitTemplate = rabbitTemplate.orElse(null);
    }

    /**
     * MQ 발행 실패 시 예외가 전파되어 주문도 롤백된다.
     */
    @Transactional
    public Order createOrder(String itemName, int quantity, int totalPrice) {
        Order order = orderRepository.save(new Order(itemName, quantity, totalPrice));
        rabbitTemplate.convertAndSend("order.exchange", "order.created",
                new OrderCreatedEvent(order.getId()));
        return order;
    }

    /**
     * MQ 발행 실패 시 예외를 삼켜서 주문은 살지만 이벤트가 유실된다.
     */
    @Transactional
    public Order createOrderSwallowingException(String itemName, int quantity, int totalPrice) {
        Order order = orderRepository.save(new Order(itemName, quantity, totalPrice));
        try {
            rabbitTemplate.convertAndSend("order.exchange", "order.created",
                    new OrderCreatedEvent(order.getId()));
        } catch (AmqpException e) {
            log.error("이벤트 발행 실패: {}", order.getId(), e);
        }
        return order;
    }
}
