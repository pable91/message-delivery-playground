package com.deliveryplayground.outbox;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 락 없이 단순 조회하는 폴러.
 * 멀티 인스턴스에서 동시에 실행되면 같은 row를 조회하여 중복 발행이 발생한다.
 */
@Service
public class SimpleOutboxPoller {

    private final OutboxRepository outboxRepository;

    public SimpleOutboxPoller(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public List<Long> pollAndPublish(long delayAfterQueryMillis) {
        List<OutboxEvent> events = outboxRepository.findByStatus("INIT");

        // 조회와 발행 사이의 간격 — 이 동안 다른 워커가 같은 row를 조회할 수 있다
        try {
            Thread.sleep(delayAfterQueryMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<Long> publishedIds = new ArrayList<>();
        for (OutboxEvent event : events) {
            publishedIds.add(event.getId());
            event.markAsSent();
        }

        return publishedIds;
    }
}
