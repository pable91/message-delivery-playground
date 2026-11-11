package com.deliveryplayground.outbox;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Outbox 폴러.
 * SELECT FOR UPDATE → 발행 → confirm 대기 → UPDATE를 하나의 트랜잭션으로 수행한다.
 * 발행 + confirm 대기 동안 DB 커넥션을 계속 점유한다.
 */
@Service
public class OutboxPoller {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void pollAndPublish(long publishDelayMillis) {
        List<OutboxEvent> events = entityManager
                .createQuery("SELECT e FROM OutboxEvent e WHERE e.status = 'INIT'", OutboxEvent.class)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();

        for (OutboxEvent event : events) {
            // 브로커 발행 + Publisher Confirm 대기 시뮬레이션
            // 이 동안 DB 커넥션을 계속 점유한다
            try {
                Thread.sleep(publishDelayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            event.markAsSent();
        }
    }
}
