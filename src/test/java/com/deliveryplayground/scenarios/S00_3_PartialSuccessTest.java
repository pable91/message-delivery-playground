package com.deliveryplayground.scenarios;

import com.deliveryplayground.external.InventoryClient;
import com.deliveryplayground.external.NotificationClient;
import com.deliveryplayground.external.PaymentClient;
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
import static org.mockito.Mockito.verify;

/**
 * #0-3. 부분 성공
 *
 * DB 트랜잭션 롤백이 외부 API 호출을 되돌리지 않는다.
 * 재고는 차감됐고 결제는 승인됐는데, 주문은 없다.
 */
class S00_3_PartialSuccessTest extends BaseIntegrationTest {

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRepository orderRepository;

    @MockBean
    InventoryClient inventoryClient;

    @MockBean
    PaymentClient paymentClient;

    @MockBean
    NotificationClient notificationClient;

    @Test
    @DisplayName("정상: 모든 외부 서비스가 정상이면 주문이 성공한다")
    void orderSucceedsWhenAllServicesAreHealthy() {
        orderService.createOrder("keyboard", 1, 89000);

        assertThat(orderRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("장애: 알림 실패 시 DB는 롤백되지만 재고 차감과 결제 승인은 되돌려지지 않는다")
    void dbRollbackDoesNotRevertExternalApiCalls() {
        doThrow(new RuntimeException("Connection timed out: notification service"))
                .when(notificationClient).sendOrderConfirmation(anyLong());

        assertThatThrownBy(() ->
                orderService.createOrder("keyboard", 1, 89000))
                .isInstanceOf(RuntimeException.class);

        // DB 주문은 롤백됐다
        assertThat(orderRepository.findAll()).isEmpty();

        // 그러나 재고 차감과 결제 승인은 이미 호출됐다 — 되돌려지지 않는다
        verify(inventoryClient).decreaseStock(anyLong());
        verify(paymentClient).processPayment(anyLong());
    }
}
