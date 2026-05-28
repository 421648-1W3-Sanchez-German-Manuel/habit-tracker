package com.tp1.habittracker.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Durable record of a state change that must be projected into Neo4j. Written in
 * the same transaction as the primary store change (for JPA entities) or
 * immediately after (for Mongo entities). The {@link com.tp1.habittracker.service.graph.GraphSyncService}
 * polls unprocessed rows and applies them to the graph.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "outbox_events",
        indexes = {
                @Index(name = "idx_outbox_pending", columnList = "processed_at, created_at")
        }
)
public class OutboxEvent {

    @Id
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private GraphEventType eventType;

    /** Primary subject of the event (user id, habit id, or requester id for friendship events). */
    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    /** Secondary subject (owner user id for habits, target user id for friendship events). */
    @Column(name = "related_id", length = 64)
    private String relatedId;

    /** Free-form payload (e.g. habit name for HABIT_CREATED). Kept as a plain String to avoid serializer coupling. */
    @Column(name = "payload", length = 1024)
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "last_error", length = 2000)
    private String lastError;
}
