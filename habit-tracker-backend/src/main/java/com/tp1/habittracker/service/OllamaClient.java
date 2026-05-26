package com.tp1.habittracker.service;

import com.tp1.habittracker.config.OllamaProperties;
import com.tp1.habittracker.exception.UpstreamBadResponseException;
import com.tp1.habittracker.exception.UpstreamServiceUnavailableException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;

@Service
public class OllamaClient {

    private final WebClient webClient;
    private final OllamaProperties properties;

    public OllamaClient(@Qualifier("ollamaWebClient") WebClient webClient, OllamaProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    public List<Double> generateEmbedding(String text) {
        Objects.requireNonNull(text, "text must not be null");
        if (text.isBlank()) {
            throw new UpstreamBadResponseException("Text must not be blank");
        }

        try {
            UpstreamEmbeddingsResponse response = webClient.post()
                    .uri("/api/embeddings")
                    .bodyValue(new EmbeddingsRequest("llama3", text))
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(body -> new UpstreamServiceUnavailableException(
                                    "Ollama returned status "
                                            + clientResponse.statusCode().value()
                                            + (body.isBlank() ? "" : (": " + truncate(body, 200)))
                            )))
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(body -> new UpstreamBadResponseException(
                                    "Ollama returned status "
                                            + clientResponse.statusCode().value()
                                            + (body.isBlank() ? "" : (": " + truncate(body, 200)))
                            )))
                    .bodyToMono(UpstreamEmbeddingsResponse.class)
                    .block(Duration.ofMillis(properties.getReadTimeoutMs()));

            if (response == null) {
                throw new UpstreamBadResponseException("Ollama returned empty response");
            }

            List<Double> embedding = response.embedding() != null && !response.embedding().isEmpty()
                    ? response.embedding()
                    : response.embeddings();

            if (embedding == null || embedding.isEmpty()) {
                throw new UpstreamBadResponseException("Ollama response did not include embedding data");
            }

            return embedding;
        } catch (UpstreamBadResponseException | UpstreamServiceUnavailableException ex) {
            throw ex;
        } catch (WebClientRequestException ex) {
            throw new UpstreamServiceUnavailableException("Ollama is unreachable", ex);
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE || ex.getStatusCode().is5xxServerError()) {
                throw new UpstreamServiceUnavailableException(
                        "Ollama returned status " + ex.getStatusCode().value(),
                        ex
                );
            }
            throw new UpstreamBadResponseException(
                    "Ollama returned status " + ex.getStatusCode().value(),
                    ex
            );
        } catch (RuntimeException ex) {
            Throwable unwrapped = Exceptions.unwrap(ex);
            if (unwrapped instanceof java.util.concurrent.TimeoutException) {
                throw new UpstreamServiceUnavailableException("Ollama request timed out", ex);
            }
            if (unwrapped instanceof UpstreamServiceUnavailableException upstreamServiceUnavailableException) {
                throw upstreamServiceUnavailableException;
            }
            if (unwrapped instanceof UpstreamBadResponseException upstreamBadResponseException) {
                throw upstreamBadResponseException;
            }
            throw new UpstreamBadResponseException("Ollama returned invalid response", ex);
        } catch (Exception ex) {
            throw new UpstreamServiceUnavailableException("Ollama request failed unexpectedly", ex);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private record EmbeddingsRequest(String model, String prompt) {
    }

    private record UpstreamEmbeddingsResponse(List<Double> embedding, List<Double> embeddings) {
    }
}