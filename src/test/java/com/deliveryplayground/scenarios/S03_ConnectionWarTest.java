package com.deliveryplayground.scenarios;

import com.deliveryplayground.order.Order;
import com.deliveryplayground.order.OrderRepository;
import com.deliveryplayground.outbox.OutboxRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * #3. 커넥션 쟁탈전
 *
 * 폴러와 비즈니스 API가 같은 커넥션 풀을 공유할 때,
 * 폴러가 커넥션을 오래 점유하면 비즈니스 API가 커넥션을 못 얻는다.
 *
 * 실제 환경에서는 @Scheduled 폴러가 SELECT FOR UPDATE → 발행 → confirm 대기를
 * 하나의 트랜잭션으로 수행하면서 커넥션을 오래 점유한다.
 * 테스트에서는 안정적인 재현을 위해 직접 커넥션을 점유하여 풀 고갈 상황을 만든다.
 */
@TestPropertySource(properties = {
        // 커넥션 풀 크기를 5개로 제한
        "spring.datasource.hikari.maximum-pool-size=5",
        // 2초 안에 커넥션을 못 얻으면 타임아웃
        "spring.datasource.hikari.connection-timeout=2000"
})
class S03_ConnectionWarTest extends BaseIntegrationTest {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OutboxRepository outboxRepository;

    @Autowired
    DataSource dataSource;

    @Test
    @DisplayName("정상: 커넥션에 여유가 있으면 비즈니스 API는 정상 동작한다")
    void businessWorksWhenConnectionsAvailable() {
        orderRepository.save(new Order("keyboard", 1, 89000));

        assertThat(orderRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("장애: 폴러가 커넥션을 점유하면 비즈니스 API가 커넥션 타임아웃된다")
    void businessTimesOutWhenPollerHoldsConnection() throws Exception {
        HikariDataSource hikariDs = (HikariDataSource) dataSource;
        int poolSize = hikariDs.getMaximumPoolSize(); // 5

        List<Connection> heldConnections = new ArrayList<>();
        try {
            // 1. 커넥션 풀의 모든 커넥션을 선점한다.
            //    실제로는 폴러의 긴 트랜잭션 + 피크 시간대 동시 요청이 이 상황을 만든다.
            for (int i = 0; i < poolSize; i++) {
                heldConnections.add(hikariDs.getConnection());
            }
            // 이 시점에서 풀에 남은 커넥션: 0개

            // 2. 비즈니스 API가 주문을 저장하려고 커넥션을 요청한다.
            //    남은 커넥션이 없으므로 2초 대기 후 타임아웃 예외가 발생한다.
            assertThatThrownBy(() ->
                    orderRepository.save(new Order("keyboard", 1, 89000))
            ).rootCause()
                    .isInstanceOf(SQLTransientConnectionException.class)
                    .hasMessageContaining("Connection is not available");
        } finally {
            // 3. 테스트 종료 후 선점한 커넥션을 풀에 반환한다.
            for (Connection conn : heldConnections) {
                conn.close();
            }
        }
    }
}
