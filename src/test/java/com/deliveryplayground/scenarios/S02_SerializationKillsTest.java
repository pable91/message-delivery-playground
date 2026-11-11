package com.deliveryplayground.scenarios;

import com.deliveryplayground.order.OrderRepository;
import com.deliveryplayground.order.SerializationOrderService;
import com.deliveryplayground.outbox.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * #2. 직렬화가 비즈니스를 죽이다
 *
 * Outbox payload 직렬화가 비즈니스 트랜잭션 안에서 실패하면
 * 주문까지 함께 롤백된다.
 */
class S02_SerializationKillsTest extends BaseIntegrationTest {

    @Autowired
    SerializationOrderService serializationOrderService;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OutboxRepository outboxRepository;

    @Test
    @DisplayName("정상: 직렬화 가능한 DTO를 사용하면 주문과 Outbox가 함께 저장된다")
    void orderAndOutboxSavedWithSerializableDto() {
        Map<String, Object> eventPayload = Map.of(
                "itemName", "keyboard",
                "quantity", 1,
                "totalPrice", 89000
        );

        serializationOrderService.createOrder("keyboard", 1, 89000, eventPayload);

        assertThat(orderRepository.findAll()).hasSize(1);
        assertThat(outboxRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("장애: 순환 참조 객체를 직렬화하면 주문까지 함께 롤백된다")
    void serializationFailureRollsBackOrder() {
        // 순환 참조 객체 생성 — JPA 양방향 연관관계에서 흔히 발생하는 상황
        CircularA a = new CircularA();
        CircularB b = new CircularB();
        a.b = b;
        b.a = a;

        assertThatThrownBy(() ->
                serializationOrderService.createOrder("keyboard", 1, 89000, a)
        ).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("직렬화 실패");

        // Outbox 직렬화 버그 때문에 주문 자체가 롤백됐다
        assertThat(orderRepository.findAll()).isEmpty();
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    /**
     * JPA 양방향 연관관계(@OneToMany ↔ @ManyToOne)의 순환 참조를 시뮬레이션
     */
    static class CircularA {
        public CircularB b;
    }

    static class CircularB {
        public CircularA a;
    }
}
