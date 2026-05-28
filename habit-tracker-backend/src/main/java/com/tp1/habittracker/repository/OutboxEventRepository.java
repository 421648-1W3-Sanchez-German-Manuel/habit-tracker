package com.tp1.habittracker.repository;

import com.tp1.habittracker.domain.model.OutboxEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("""
            SELECT e FROM OutboxEvent e
            WHERE e.processedAt IS NULL
              AND e.attempts < :maxAttempts
            ORDER BY e.createdAt ASC
            """)
    List<OutboxEvent> findPending(@Param("maxAttempts") int maxAttempts, Pageable pageable);
}
