package com.tp1.habittracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.tp1.habittracker.repository.HabitLogRepository;
import com.tp1.habittracker.repository.HabitRepository;
import com.tp1.habittracker.repository.OutboxEventRepository;
import com.tp1.habittracker.repository.UserRepository;
import com.tp1.habittracker.repository.graph.HabitGraphRepository;
import com.tp1.habittracker.repository.graph.UserGraphRepository;
import com.tp1.habittracker.service.OllamaClient;

@SpringBootTest(
    properties = {
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration," +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration," +
            "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
            "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration," +
            "org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.neo4j.Neo4jRepositoriesAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.neo4j.Neo4jReactiveDataAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.neo4j.Neo4jReactiveRepositoriesAutoConfiguration",
        "app.seed.enabled=false",
        "app.jwt.secret=test-jwt-secret-at-least-32-characters-long",
        "app.jwt.expiration-ms=3600000"
    }
)
class HabitTrackerApiApplicationTests {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private HabitRepository habitRepository;

    @MockBean
    private HabitLogRepository habitLogRepository;

    @MockBean
    private OutboxEventRepository outboxEventRepository;

    @MockBean
    private UserGraphRepository userGraphRepository;

    @MockBean
    private HabitGraphRepository habitGraphRepository;

    @MockBean
    private OllamaClient ollamaClient;

    @Test
    void contextLoads() {
    }
}
