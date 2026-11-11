package com.deliveryplayground.scenarios;

import com.deliveryplayground.external.PaymentClient;
import com.deliveryplayground.order.OrderRepository;
import com.deliveryplayground.order.OutboxOrderService;
import com.deliveryplayground.outbox.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

/**
 * #1. 유령 이벤트
 *
 * 비즈니스 트랜잭션이 롤백됐는데 Outbox 이벤트는 커밋되어 발행된다.
 * REQUIRES_NEW로 분리한 순간 정합성이 깨진다.
 */
class S01_PhantomEventTest extends BaseIntegrationTest {

    @Autowired
    OutboxOrderService outboxOrderService;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OutboxRepository outboxRepository;

    @MockBean
    PaymentClient paymentClient;

    @Test
    @DisplayName("정상: 모든 단계 성공 시 주문과 Outbox 이벤트가 함께 저장된다")
    void orderAndOutboxSavedTogether() {
        outboxOrderService.createOrder("keyboard", 1, 89000);

        assertThat(orderRepository.findAll()).hasSize(1);
        assertThat(outboxRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("장애: 결제 실패 → 주문 롤백 → Outbox에는 유령 이벤트가 남는다")
    void phantomEventRemains() {
        doThrow(new RuntimeException("Payment server timeout"))
                .when(paymentClient).processPayment(anyLong());

        assertThatThrownBy(() ->
                outboxOrderService.createOrder("keyboard", 1, 89000)
        ).isInstanceOf(RuntimeException.class);

        // 주문은 롤백됐다
        assertThat(orderRepository.findAll()).isEmpty();

        // 하지만 Outbox 이벤트는 REQUIRES_NEW로 이미 커밋됐다 — 유령 이벤트
        assertThat(outboxRepository.findAll()).hasSize(1);
    }
}
