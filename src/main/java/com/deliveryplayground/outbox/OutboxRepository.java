package com.deliveryplayground.outbox;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatus(String status);

    @Query(value = "SELECT * FROM outbox_events WHERE status = 'INIT' ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEvent> findInitEventsForUpdateSkipLocked();
}
