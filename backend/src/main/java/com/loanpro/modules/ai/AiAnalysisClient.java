package com.loanpro.modules.ai;

import com.loanpro.config.AppProperties;
import com.loanpro.modules.ai.dto.AiAnalyzeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Component
public class AiAnalysisClient {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisClient.class);

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final URI analyzeUri;
    private final Duration timeout;

    public AiAnalysisClient(AppProperties properties, JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
        this.timeout = Duration.ofSeconds(Math.max(1, properties.ai().timeoutSeconds()));
        this.analyzeUri = URI.create(trimSlash(properties.ai().serviceUrl()) + "/api/ai/analyze-loan");
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public Optional<AiAnalyzeResponse> analyze(Map<String, Object> payload) {
        try {
            String requestJson = jsonMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder(analyzeUri)
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("AI analysis service returned {}: {}", response.statusCode(), abbreviate(response.body()));
                return Optional.empty();
            }
            String body = response.body();
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(jsonMapper.readValue(body, AiAnalyzeResponse.class));
        } catch (Exception ex) {
            log.warn("AI analysis service is unavailable: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 300 ? value : value.substring(0, 300) + "...";
    }
}
