package com.tp1.habittracker.config;

import jakarta.persistence.EntityManagerFactory;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableNeo4jRepositories(
        basePackages = "com.tp1.habittracker.repository.graph",
        transactionManagerRef = "neo4jTransactionManager"
)
public class Neo4jConfig {

    private static final Logger log = LoggerFactory.getLogger(Neo4jConfig.class);

    /**
     * Explicit Driver bean so we bypass Spring Boot's @ConfigurationProperties
     * binding for spring.neo4j.authentication.*, which silently falls back to
     * AuthTokens.none() when the dotenv-sourced env vars aren't picked up by the
     * Binder. @Value resolves directly against the Environment (including our
     * SystemEnvironmentPropertySource added by DotEnvPostProcessor) and works
     * reliably.
     *
     * Spring Boot's Neo4jAutoConfiguration carries @ConditionalOnMissingBean on its
     * own driver @Bean, so it detects this user-defined bean and skips creating a
     * second one — no duplication risk.
     */
    @Bean
    public Driver neo4jDriver(
            @Value("${SPRING_NEO4J_URI}") String uri,
            @Value("${SPRING_NEO4J_AUTHENTICATION_USERNAME:}") String username,
            @Value("${SPRING_NEO4J_AUTHENTICATION_PASSWORD:}") String password) {
        if (!username.isEmpty()) {
            log.info("Neo4j driver configured with basic auth for user '{}'", username);
            return GraphDatabase.driver(uri, AuthTokens.basic(username, password));
        }
        log.warn("SPRING_NEO4J_AUTHENTICATION_USERNAME is not set — connecting to Neo4j without auth. "
                + "This will fail if the server has auth enabled.");
        return GraphDatabase.driver(uri, AuthTokens.none());
    }

    @Bean("transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean("neo4jTransactionManager")
    public Neo4jTransactionManager neo4jTransactionManager(Driver driver,
                                                           DatabaseSelectionProvider databaseSelectionProvider) {
        return new Neo4jTransactionManager(driver, databaseSelectionProvider);
    }
}
