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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;

/**
 * #0-2. 느린 동반자
 *
 * 동기 직렬 호출의 총 응답 시간 = 각 호출 시간의 합.
 * 가장 느린 서비스가 전체 응답 시간을 결정한다.
 */
class S00_2_SlowCompanionTest extends BaseIntegrationTest {

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
    @DisplayName("정상: 모든 외부 서비스가 빠르면 주문이 빠르게 성공한다")
    void orderSucceedsQuicklyWhenAllServicesAreFast() {
        long start = System.currentTimeMillis();

        orderService.createOrder("keyboard", 1, 89000);

        long elapsed = System.currentTimeMillis() - start;

        assertThat(orderRepository.findAll()).hasSize(1);
        assertThat(elapsed).isLessThan(500); // Mock이므로 거의 즉시
    }

    @Test
    @DisplayName("장애: 결제 서비스가 느려지면 전체 응답 시간이 느려진다")
    void slowPaymentServiceSlowsDownEntireOrder() {
        // 재고: 50ms, 결제: 2000ms (프로모션 트래픽), 알림: 150ms
        doAnswer(invocation -> { Thread.sleep(50); return null; })
                .when(inventoryClient).decreaseStock(anyLong());
        doAnswer(invocation -> { Thread.sleep(2000); return null; })
                .when(paymentClient).processPayment(anyLong());
        doAnswer(invocation -> { Thread.sleep(150); return null; })
                .when(notificationClient).sendOrderConfirmation(anyLong());

        long start = System.currentTimeMillis();

        orderService.createOrder("keyboard", 1, 89000);

        long elapsed = System.currentTimeMillis() - start;

        // 주문 자체는 성공하지만, 총 응답 시간이 2초를 넘긴다
        assertThat(orderRepository.findAll()).hasSize(1);
        assertThat(elapsed).isGreaterThanOrEqualTo(2000);
    }
}
