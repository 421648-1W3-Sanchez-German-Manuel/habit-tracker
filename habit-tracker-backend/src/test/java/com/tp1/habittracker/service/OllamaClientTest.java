package com.tp1.habittracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tp1.habittracker.config.OllamaProperties;
import com.tp1.habittracker.exception.UpstreamBadResponseException;
import com.tp1.habittracker.exception.UpstreamServiceUnavailableException;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

class OllamaClientTest {

    private OllamaProperties properties;

    @BeforeEach
    void setUp() {
        properties = new OllamaProperties();
        properties.setBaseUrl("http://ollama:11434");
        properties.setReadTimeoutMs(10_000);
        properties.setConnectTimeoutMs(2_000);
    }

    @Test
    void generateEmbeddingReturnsEmbeddingWhenValidPayloadIsReturned() {
        ExchangeFunction exchangeFunction = request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .body("{\"embedding\":[0.1,0.2,0.3]}")
                        .build()
        );

        OllamaClient client = new OllamaClient(
                WebClient.builder().exchangeFunction(exchangeFunction).build(),
                properties
        );

        List<Double> embedding = client.generateEmbedding("hola mundo");

        assertEquals(List.of(0.1, 0.2, 0.3), embedding);
    }

    @Test
    void generateEmbeddingThrowsServiceUnavailableWhenOllamaIsUnreachable() {
        ExchangeFunction exchangeFunction = request -> Mono.error(new WebClientRequestException(
                new IOException("Connection refused"),
                HttpMethod.POST,
                URI.create("http://ollama:11434/api/embeddings"),
                HttpHeaders.EMPTY
        ));

        OllamaClient client = new OllamaClient(
                WebClient.builder().exchangeFunction(exchangeFunction).build(),
                properties
        );

        assertThrows(UpstreamServiceUnavailableException.class, () -> client.generateEmbedding("hola"));
    }

    @Test
    void generateEmbeddingThrowsBadResponseWhenEmbeddingFieldIsMissing() {
        ExchangeFunction exchangeFunction = request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .body("{}")
                        .build()
        );

        OllamaClient client = new OllamaClient(
                WebClient.builder().exchangeFunction(exchangeFunction).build(),
                properties
        );

        assertThrows(UpstreamBadResponseException.class, () -> client.generateEmbedding("texto"));
    }
}