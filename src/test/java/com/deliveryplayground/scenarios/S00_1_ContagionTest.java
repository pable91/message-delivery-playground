package com.deliveryplayground.scenarios;

import com.deliveryplayground.external.NotificationClient;
import com.deliveryplayground.order.OrderRepository;
import com.deliveryplayground.order.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

/**
 * #0-1. 전염
 *
 * 동기 호출은 호출 대상의 장애를 호출자에게 전염시킨다.
 * 알림 서비스가 죽으면 주문 서비스도 죽는다.
 */
class S00_1_ContagionTest extends BaseIntegrationTest {

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRepository orderRepository;

    @MockBean
    NotificationClient notificationClient;

    @Test
    @DisplayName("알림 서비스 장애 시 주문 생성도 실패한다")
    void externalFailureCascadesToOrderCreation() {
        // 알림 서비스가 타임아웃으로 예외를 던진다
        doThrow(new RuntimeException("Connection timed out: notification service"))
                .when(notificationClient).sendOrderConfirmation(anyLong());

        // 주문 생성 시도 — 알림과 무관한 주문인데도 실패한다
        assertThatThrownBy(() ->
                orderService.createOrder("keyboard", 1, 89000))
                .isInstanceOf(RuntimeException.class);

        // DB에 주문이 남아있지 않다 — 트랜잭션이 롤백됐다
        assertThat(orderRepository.findAll()).isEmpty();
    }
}
