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
        "spring.datasource.hikari.maximum-pool-size=5",
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
        int poolSize = hikariDs.getMaximumPoolSize();

        // 폴러 + 동시 요청이 모든 커넥션을 점유하는 상황 시뮬레이션
        // 실제로는 폴러의 SELECT FOR UPDATE → confirm 대기가 커넥션을 오래 물고 있음
        List<Connection> heldConnections = new ArrayList<>();
        try {
            for (int i = 0; i < poolSize; i++) {
                heldConnections.add(hikariDs.getConnection());
            }

            // 비즈니스 API가 커넥션을 요청 → 모두 점유 중 → 타임아웃
            assertThatThrownBy(() ->
                    orderRepository.save(new Order("keyboard", 1, 89000))
            ).rootCause()
                    .isInstanceOf(SQLTransientConnectionException.class)
                    .hasMessageContaining("Connection is not available");
        } finally {
            for (Connection conn : heldConnections) {
                conn.close();
            }
        }
    }
}
