package com.deliveryplayground.outbox;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SELECT FOR UPDATE SKIP LOCKED를 사용하는 폴러.
 * 중복 발행은 방지하지만, 같은 aggregate의 이벤트가 다른 워커에 분배되면 순서가 역전될 수 있다.
 */
@Service
public class SkipLockedOutboxPoller {

    private final OutboxRepository outboxRepository;

    public SkipLockedOutboxPoller(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public String pollOneAndPublish(long publishDelayMillis) {
        List<OutboxEvent> events = outboxRepository.findInitEventsForUpdateSkipLocked();

        if (events.isEmpty()) {
            return null;
        }

        OutboxEvent event = events.get(0);

        // 브로커 발행 시뮬레이션 — 네트워크 상태에 따라 지연될 수 있다
        try {
            Thread.sleep(publishDelayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        event.markAsSent();
        return event.getEventType();
    }
}
