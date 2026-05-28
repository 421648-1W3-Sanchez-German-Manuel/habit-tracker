package com.tp1.habittracker.repository.graph;

import com.tp1.habittracker.domain.graph.HabitNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

public interface HabitGraphRepository extends Neo4jRepository<HabitNode, String> {

    @Query("""
            MERGE (h:Habit {id: $habitId})
              ON CREATE SET h.name = $name
              ON MATCH  SET h.name = $name
            RETURN h
            """)
    HabitNode upsertHabitNode(@Param("habitId") String habitId, @Param("name") String name);

    @Query("MATCH (h:Habit {id: $habitId}) DETACH DELETE h")
    void deleteHabitNode(@Param("habitId") String habitId);
}
