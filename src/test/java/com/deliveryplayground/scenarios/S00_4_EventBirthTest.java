package com.deliveryplayground.scenarios;

import com.deliveryplayground.order.AsyncOrderService;
import com.deliveryplayground.order.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * #0-4. 이벤트의 탄생
 *
 * MQ를 도입해도 "DB 커밋 + MQ 발행"이 원자적이지 않으면
 * 이벤트가 유실되거나 비즈니스가 죽는다.
 */
class S00_4_EventBirthTest extends BaseIntegrationTest {

    @Autowired
    AsyncOrderService asyncOrderService;

    @Autowired
    OrderRepository orderRepository;

    @MockBean
    RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("정상: 브로커가 정상이면 주문 성공 + 이벤트 발행")
    void orderSucceedsAndEventPublishedWhenBrokerIsHealthy() {
        asyncOrderService.createOrder("keyboard", 1, 89000);

        assertThat(orderRepository.findAll()).hasSize(1);
        verify(rabbitTemplate).convertAndSend(
                eq("order.exchange"), eq("order.created"), any(Object.class));
    }

    @Test
    @DisplayName("장애(예외 전파): 브로커 다운 시 주문도 롤백된다 — 전염 반복")
    void brokerFailureCascadesToOrderCreation() {
        doThrow(new AmqpConnectException(new RuntimeException("Connection refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertThatThrownBy(() ->
                asyncOrderService.createOrder("keyboard", 1, 89000))
                .isInstanceOf(AmqpConnectException.class);

        // 브로커 장애인데 주문까지 롤백됐다
        assertThat(orderRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("장애(예외 삼킴): 주문은 살지만 이벤트가 유실된다")
    void orderSurvivesButEventIsLost() {
        doThrow(new AmqpConnectException(new RuntimeException("Connection refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        asyncOrderService.createOrderSwallowingException("keyboard", 1, 89000);

        // 주문은 살았다
        assertThat(orderRepository.findAll()).hasSize(1);

        // 하지만 이벤트는 유실됐다 — 재고 차감, 알림 발송이 일어나지 않는다
        // 발행에 실패했다는 사실이 DB에 기록되지 않으므로 재시도할 방법도 없다
    }
}
