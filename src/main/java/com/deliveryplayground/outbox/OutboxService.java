package com.deliveryplayground.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 의도적으로 잘못된 설계: REQUIRES_NEW로 Outbox를 저장한다.
 * 비즈니스 트랜잭션과 별도 트랜잭션이므로 정합성이 깨질 수 있다.
 */
@Service
public class OutboxService {

    private final OutboxRepository outboxRepository;

    public OutboxService(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(OutboxEvent event) {
        outboxRepository.save(event);
    }
}
