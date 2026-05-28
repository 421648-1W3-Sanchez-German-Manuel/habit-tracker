package com.tp1.habittracker.domain.graph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Node("Habit")
public class HabitNode {

    @Id
    private String id;

    private String name;
}
