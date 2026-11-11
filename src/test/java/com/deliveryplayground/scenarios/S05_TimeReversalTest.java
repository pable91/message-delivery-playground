package com.deliveryplayground.scenarios;

import com.deliveryplayground.outbox.OutboxEvent;
import com.deliveryplayground.outbox.OutboxRepository;
import com.deliveryplayground.outbox.SkipLockedOutboxPoller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * #5. 역전된 시간
 *
 * SKIP LOCKED는 중복을 방지하지만, 같은 aggregate의 이벤트가
 * 다른 워커에 분배되면 순서가 역전될 수 있다.
 */
class S05_TimeReversalTest extends BaseIntegrationTest {

    @Autowired
    SkipLockedOutboxPoller skipLockedOutboxPoller;

    @Autowired
    OutboxRepository outboxRepository;

    @Test
    @DisplayName("정상: 단일 폴러면 이벤트가 생성 순서대로 발행된다")
    void singlePollerPreservesOrder() {
        outboxRepository.save(new OutboxEvent("Order", 5577L, "OrderCreated", "{\"orderId\":5577}"));
        outboxRepository.save(new OutboxEvent("Order", 5577L, "OrderPaid", "{\"orderId\":5577}"));

        // 단일 폴러가 순서대로 처리
        String first = skipLockedOutboxPoller.pollOneAndPublish(0);
        String second = skipLockedOutboxPoller.pollOneAndPublish(0);

        assertThat(first).isEqualTo("OrderCreated");
        assertThat(second).isEqualTo("OrderPaid");
    }

    @Test
    @DisplayName("장애: 두 워커가 SKIP LOCKED로 폴링하면 이벤트 순서가 역전된다")
    void orderReversedWithSkipLocked() throws Exception {
        // 같은 aggregate(Order-5577)의 이벤트 2건: 생성 → 결제 순서로 INSERT
        outboxRepository.save(new OutboxEvent("Order", 5577L, "OrderCreated", "{\"orderId\":5577}"));
        outboxRepository.save(new OutboxEvent("Order", 5577L, "OrderPaid", "{\"orderId\":5577}"));

        // 발행 순서를 기록하는 공유 리스트
        List<String> publishOrder = new CopyOnWriteArrayList<>();

        // Worker A: id=1(OrderCreated)을 잡지만 네트워크 지연으로 발행이 느리다
        Thread workerA = new Thread(() -> {
            String eventType = skipLockedOutboxPoller.pollOneAndPublish(2000);
            if (eventType != null) publishOrder.add(eventType);
        });

        // Worker B: id=1은 잠겨있으므로 건너뛰고, id=2(OrderPaid)를 잡아서 즉시 발행한다
        Thread workerB = new Thread(() -> {
            try {
                Thread.sleep(200); // Worker A가 먼저 락을 잡도록 대기
            } catch (InterruptedException ignored) {
            }
            String eventType = skipLockedOutboxPoller.pollOneAndPublish(0);
            if (eventType != null) publishOrder.add(eventType);
        });

        workerA.start();
        workerB.start();
        workerA.join(5000);
        workerB.join(5000);

        // OrderPaid가 먼저 발행되고, OrderCreated가 나중에 발행됐다 — 순서 역전!
        assertThat(publishOrder).containsExactly("OrderPaid", "OrderCreated");
    }
}
