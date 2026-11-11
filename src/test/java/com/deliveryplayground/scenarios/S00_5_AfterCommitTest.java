package com.deliveryplayground.scenarios;

import com.deliveryplayground.order.AfterCommitOrderService;
import com.deliveryplayground.order.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * #0-5. DB 커밋은 됐는데
 *
 * @TransactionalEventListener(AFTER_COMMIT)은 "커밋 후 1회 시도"일 뿐,
 * 발행을 보장하지 않는다.
 */
class S00_5_AfterCommitTest extends BaseIntegrationTest {

    @Autowired
    AfterCommitOrderService afterCommitOrderService;

    @Autowired
    OrderRepository orderRepository;

    @MockBean
    RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("정상: 브로커가 정상이면 커밋 후 이벤트가 발행된다")
    void eventPublishedAfterCommitWhenBrokerIsHealthy() {
        afterCommitOrderService.createOrder("keyboard", 1, 89000);

        assertThat(orderRepository.findAll()).hasSize(1);
        verify(rabbitTemplate).convertAndSend(
                anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("장애: 커밋 후 브로커 다운 — 주문은 있지만 이벤트는 영구 유실된다")
    void eventLostAfterCommitWhenBrokerIsDown() {
        doThrow(new AmqpConnectException(new RuntimeException("Connection refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        afterCommitOrderService.createOrder("keyboard", 1, 89000);

        // 주문은 DB에 저장됐다 — 트랜잭션은 이미 커밋된 후이므로 롤백되지 않는다
        assertThat(orderRepository.findAll()).hasSize(1);

        // 하지만 이벤트는 발행되지 않았고, 재시도할 방법이 없다
        // "발행해야 한다"는 기록이 어디에도 없으므로 이벤트는 영구 유실된다
    }
}
