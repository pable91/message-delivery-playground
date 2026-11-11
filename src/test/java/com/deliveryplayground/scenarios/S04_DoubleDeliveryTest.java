package com.deliveryplayground.scenarios;

import com.deliveryplayground.outbox.OutboxEvent;
import com.deliveryplayground.outbox.OutboxRepository;
import com.deliveryplayground.outbox.SimpleOutboxPoller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * #4. 이중 배달
 *
 * 멀티 인스턴스에서 같은 폴러가 동시에 실행되면,
 * SELECT FOR UPDATE 없이는 같은 row를 조회하여 중복 발행이 발생한다.
 */
class S04_DoubleDeliveryTest extends BaseIntegrationTest {

    @Autowired
    SimpleOutboxPoller simpleOutboxPoller;

    @Autowired
    OutboxRepository outboxRepository;

    @Test
    @DisplayName("정상: 단일 폴러면 3건의 이벤트가 정확히 3번 발행된다")
    void singlePollerPublishesExactly() {
        outboxRepository.save(new OutboxEvent("Order", 1L, "OrderCreated", "{\"orderId\":1}"));
        outboxRepository.save(new OutboxEvent("Order", 2L, "OrderCreated", "{\"orderId\":2}"));
        outboxRepository.save(new OutboxEvent("Order", 3L, "OrderCreated", "{\"orderId\":3}"));

        List<Long> published = simpleOutboxPoller.pollAndPublish(0);

        assertThat(published).hasSize(3);
    }

    @Test
    @DisplayName("장애: 두 폴러가 동시에 실행되면 같은 이벤트가 중복 발행된다")
    void duplicatePublishingWithoutLock() throws Exception {
        // 3건의 Outbox 이벤트를 미리 넣어둔다
        outboxRepository.save(new OutboxEvent("Order", 1L, "OrderCreated", "{\"orderId\":1}"));
        outboxRepository.save(new OutboxEvent("Order", 2L, "OrderCreated", "{\"orderId\":2}"));
        outboxRepository.save(new OutboxEvent("Order", 3L, "OrderCreated", "{\"orderId\":3}"));

        // 두 워커가 발행한 이벤트 ID를 수집
        List<Long> workerAPublished = new CopyOnWriteArrayList<>();
        List<Long> workerBPublished = new CopyOnWriteArrayList<>();
        CountDownLatch startLatch = new CountDownLatch(1);

        // Worker A: 조회 후 500ms 대기 — 이 동안 Worker B도 같은 row를 조회한다
        Thread workerA = new Thread(() -> {
            try {
                startLatch.await();
                workerAPublished.addAll(simpleOutboxPoller.pollAndPublish(500));
            } catch (InterruptedException ignored) {
            }
        });

        // Worker B: 동시에 실행 — 같은 INIT 상태의 row를 조회한다
        Thread workerB = new Thread(() -> {
            try {
                startLatch.await();
                workerBPublished.addAll(simpleOutboxPoller.pollAndPublish(500));
            } catch (InterruptedException ignored) {
            }
        });

        workerA.start();
        workerB.start();
        startLatch.countDown(); // 두 워커 동시 시작

        workerA.join(5000);
        workerB.join(5000);

        // 이벤트는 3건인데, 두 워커가 같은 row를 조회해서 총 6건이 발행됐다
        int totalPublished = workerAPublished.size() + workerBPublished.size();
        assertThat(totalPublished).isGreaterThan(3);

        // 두 워커가 동일한 이벤트를 발행했다 — 중복
        List<Long> duplicates = new ArrayList<>(workerAPublished);
        duplicates.retainAll(workerBPublished);
        assertThat(duplicates).isNotEmpty();
    }
}
