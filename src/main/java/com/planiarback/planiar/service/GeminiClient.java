package com.planiarback.planiar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@Component
public class GeminiClient {
    private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);

    @Value("${gemini.apiKey:}")
    private String apiKey;

    @Value("${gemini.model:gemini-1}")
    private String model;

    // Base endpoint, configurable for different deployments
    @Value("${gemini.endpoint:https://generative.googleapis.com/v1}")
    private String endpoint;

    @Value("${gemini.useApiKey:false}")
    private boolean useApiKey;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Call Gemini (Generative API) with a text prompt. Returns the model's text output if available.
     * The implementation is intentionally generic: the exact REST shape may vary by deployment.
     * Configure `gemini.apiKey` and `gemini.model` in application.properties or env.
     */
    public Optional<String> generateText(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            logger.debug("Gemini API key not configured, skipping external AI call");
            return Optional.empty();
        }

        try {
            // Construct a generic request expected by many Gemini REST wrappers.
            // Payload asks model for a JSON-only response describing assignments.
            ObjectNodeWrapper wrapper = new ObjectNodeWrapper(mapper);
            wrapper.put("prompt", prompt);
            wrapper.put("maxOutputTokens", 16000);

            String body = wrapper.toString();

            String url = endpoint;
            // If endpoint looks like base, append model path
            if (!endpoint.contains(model)) {
                if (endpoint.endsWith("/")) url = endpoint + "v1/models/" + model + ":generateText";
                else url = endpoint + "/v1/models/" + model + ":generateText";
            }

            // If configured to use API key mode, attach key as query param; otherwise use Bearer token
            if (useApiKey || (apiKey != null && apiKey.startsWith("AIza"))) {
                try {
                    String encoded = java.net.URLEncoder.encode(apiKey, java.nio.charset.StandardCharsets.UTF_8.toString());
                    if (url.contains("?")) url = url + "&key=" + encoded;
                    else url = url + "?key=" + encoded;
                } catch (Exception e) {
                    // fallback: append raw
                    if (url.contains("?")) url = url + "&key=" + apiKey;
                    else url = url + "?key=" + apiKey;
                }
            }

            HttpRequest.Builder rb = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json");

            if (!(useApiKey || (apiKey != null && apiKey.startsWith("AIza")))) {
                rb.header("Authorization", "Bearer " + apiKey);
            }

            HttpRequest req = rb.POST(HttpRequest.BodyPublishers.ofString(body)).build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                String respBody = resp.body();
                // Try to extract text from several possible fields
                JsonNode root = mapper.readTree(respBody);
                // Common patterns: candidates[].content, outputs[].content, text
                if (root.has("candidates") && root.get("candidates").isArray() && root.get("candidates").size() > 0) {
                    JsonNode c0 = root.get("candidates").get(0);
                    if (c0.has("content")) return Optional.of(c0.get("content").asText());
                }
                if (root.has("outputs") && root.get("outputs").isArray() && root.get("outputs").size() > 0) {
                    JsonNode o0 = root.get("outputs").get(0);
                    if (o0.has("content")) return Optional.of(o0.get("content").asText());
                    if (o0.has("text")) return Optional.of(o0.get("text").asText());
                }
                if (root.has("text")) return Optional.of(root.get("text").asText());
                // fallback to raw response
                return Optional.of(respBody);
            } else {
                logger.warn("Gemini call returned status {}: {}", resp.statusCode(), resp.body());
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("Error calling Gemini: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    // tiny helper to build object nodes without importing Jackson types in this file repeatedly
    private static class ObjectNodeWrapper {
        private final ObjectMapper mapper;
        private final JsonNode node;
        private final com.fasterxml.jackson.databind.node.ObjectNode on;
        ObjectNodeWrapper(ObjectMapper mapper) {
            this.mapper = mapper;
            this.on = mapper.createObjectNode();
            this.node = on;
        }
        void put(String k, String v) { on.put(k, v); }
        void put(String k, int v) { on.put(k, v); }
        @Override public String toString() { try { return mapper.writeValueAsString(on); } catch (Exception e) { return "{}";} }
    }
}
