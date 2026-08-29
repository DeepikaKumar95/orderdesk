package com.orderdesk.order.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

    @Modifying
    @Query("update OutboxEvent e set e.publishedAt = :now where e.id in :ids")
    int markPublished(@Param("ids") List<UUID> ids, @Param("now") Instant now);
}
