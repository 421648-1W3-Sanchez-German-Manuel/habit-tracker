package com.tp1.habittracker.service.graph;

import com.tp1.habittracker.config.GraphSyncProperties;
import com.tp1.habittracker.domain.model.OutboxEvent;
import com.tp1.habittracker.repository.OutboxEventRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Polls the outbox table and projects each pending event into Neo4j. Each event is
 * applied in its own transaction via {@link OutboxEventDispatcher} so a single
 * failure doesn't poison the batch.
 *
 * Sync model: best-effort with durability. The outbox row is written in the same
 * JPA transaction as the primary store change (for User events) or immediately
 * after the Mongo write (for Habit events). On Neo4j-side failure, attempts is
 * incremented and the event is retried until {@link GraphSyncProperties#getMaxAttempts()}.
 */
@Service
@RequiredArgsConstructor
public class GraphSyncService {

    private static final Logger log = LoggerFactory.getLogger(GraphSyncService.class);

    private final OutboxEventRepository outboxRepository;
    private final OutboxEventDispatcher dispatcher;
    private final GraphSyncProperties properties;

    @Scheduled(fixedDelayString = "${app.graph.sync.poll-ms:2000}")
    public void drain() {
        List<OutboxEvent> pending = outboxRepository.findPending(
                properties.getMaxAttempts(),
                PageRequest.of(0, properties.getBatchSize())
        );

        if (pending.isEmpty()) {
            return;  // nothing to do — keep this path silent to avoid log spam
        }

        log.info("Graph sync: processing {} pending outbox event(s)", pending.size());

        for (OutboxEvent event : pending) {
            try {
                dispatcher.applyAndMarkProcessed(event);
            } catch (Exception ex) {
                log.warn("Graph sync failed for event {} (type={}, attempts={}): {}",
                        event.getId(), event.getEventType(), event.getAttempts() + 1, ex.getMessage());
                try {
                    dispatcher.recordFailure(event, ex.getMessage());
                } catch (Exception recordEx) {
                    log.error("Could not persist failure for event {} — attempts counter not incremented: {}",
                            event.getId(), recordEx.getMessage());
                }
            }
        }
    }
}
