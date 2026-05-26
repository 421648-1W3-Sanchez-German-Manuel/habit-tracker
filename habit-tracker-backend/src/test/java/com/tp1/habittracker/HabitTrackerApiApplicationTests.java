package com.tp1.habittracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.tp1.habittracker.repository.HabitLogRepository;
import com.tp1.habittracker.repository.HabitRepository;
import com.tp1.habittracker.repository.UserRepository;
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
            "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
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
    private OllamaClient ollamaClient;

    @Test
    void contextLoads() {
    }
}
