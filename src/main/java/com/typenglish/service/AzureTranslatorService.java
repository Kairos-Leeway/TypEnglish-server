package com.typenglish.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AzureTranslatorService {
    private static final Logger log = LoggerFactory.getLogger(AzureTranslatorService.class);
    private static final String TARGET_LANGUAGE = "zh-Hans";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String region;
    private final String endpoint;

    @Autowired
    public AzureTranslatorService(
            @Value("${translator.azure.key:}") String apiKey,
            @Value("${translator.azure.region:eastasia}") String region,
            @Value("${translator.azure.endpoint:https://api.cognitive.microsofttranslator.com}") String endpoint) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build(),
                new ObjectMapper(), apiKey, region, endpoint);
    }

    AzureTranslatorService(HttpClient httpClient, ObjectMapper objectMapper,
                           String apiKey, String region, String endpoint) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.region = region == null ? "" : region.trim();
        String configuredEndpoint = endpoint == null ? "" : endpoint.trim();
        this.endpoint = configuredEndpoint.isBlank()
                ? "https://api.cognitive.microsofttranslator.com"
                : configuredEndpoint.replaceAll("/+$", "");
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public Map<String, Object> lookup(String word, String language) {
        if (!isConfigured() || word == null || word.isBlank()) return null;
        String sourceLanguage = normalizeLanguage(language);
        try {
            if ("en".equals(sourceLanguage)) {
                DictionaryHit hit = parseDictionaryResponse(post(
                        "/dictionary/lookup?api-version=3.0&from=en&to=" + TARGET_LANGUAGE,
                        List.of(Map.of("Text", word))));
                if (hit != null) {
                    String example = null;
                    try {
                        example = parseExampleResponse(post(
                                "/dictionary/examples?api-version=3.0&from=en&to=" + TARGET_LANGUAGE,
                                List.of(Map.of(
                                        "Text", hit.normalizedSource(),
                                        "Translation", hit.normalizedTarget()))));
                    } catch (Exception e) {
                        log.debug("Azure dictionary example lookup failed for '{}': {}", word, e.getMessage());
                    }
                    return dictionaryResult(word, hit, example);
                }
            }

            String translated = parseTranslationResponse(post(
                    "/translate?api-version=3.0&from=" + encode(sourceLanguage) + "&to=" + TARGET_LANGUAGE,
                    List.of(Map.of("Text", word))));
            if (translated == null || translated.isBlank()) return null;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("found", true);
            result.put("word", word);
            result.put("phonetic", null);
            result.put("translation", translated);
            result.put("partOfSpeech", null);
            result.put("example", null);
            return result;
        } catch (Exception e) {
            log.warn("Azure Translator lookup failed for '{}': {}", word, e.getMessage());
            return null;
        }
    }

    DictionaryHit parseDictionaryResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        if (!root.isArray() || root.isEmpty()) return null;
        JsonNode entry = root.get(0);
        JsonNode translations = entry.path("translations");
        if (!translations.isArray() || translations.isEmpty()) return null;

        JsonNode best = null;
        double bestConfidence = Double.NEGATIVE_INFINITY;
        for (JsonNode translation : translations) {
            String displayTarget = textOrNull(translation, "displayTarget");
            if (displayTarget == null) continue;
            double confidence = translation.path("confidence").asDouble(0);
            if (best == null || confidence > bestConfidence) {
                best = translation;
                bestConfidence = confidence;
            }
        }
        if (best == null) return null;

        String normalizedSource = firstNonBlank(
                textOrNull(entry, "normalizedSource"), textOrNull(entry, "displaySource"));
        String normalizedTarget = firstNonBlank(
                textOrNull(best, "normalizedTarget"), textOrNull(best, "displayTarget"));
        String displayTarget = textOrNull(best, "displayTarget");
        if (normalizedSource == null || normalizedTarget == null || displayTarget == null) return null;
        return new DictionaryHit(normalizedSource, normalizedTarget, displayTarget,
                normalizePartOfSpeech(textOrNull(best, "posTag")));
    }

    String parseExampleResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        if (!root.isArray() || root.isEmpty()) return null;
        JsonNode examples = root.get(0).path("examples");
        if (!examples.isArray() || examples.isEmpty()) return null;
        JsonNode example = examples.get(0);
        String source = joinExample(example, "sourcePrefix", "sourceTerm", "sourceSuffix");
        String target = joinExample(example, "targetPrefix", "targetTerm", "targetSuffix");
        if (source == null) return null;
        return target == null ? source : source + " (" + target + ")";
    }

    String parseTranslationResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        if (!root.isArray() || root.isEmpty()) return null;
        JsonNode translations = root.get(0).path("translations");
        if (!translations.isArray() || translations.isEmpty()) return null;
        return textOrNull(translations.get(0), "text");
    }

    private String post(String path, Object body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + path))
                .timeout(Duration.ofSeconds(6))
                .header("Ocp-Apim-Subscription-Key", apiKey)
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        if (!region.isBlank()) builder.header("Ocp-Apim-Subscription-Region", region);

        HttpResponse<String> response = httpClient.send(
                builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    private Map<String, Object> dictionaryResult(String word, DictionaryHit hit, String example) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("found", true);
        result.put("word", word);
        result.put("phonetic", null);
        result.put("translation", hit.displayTarget());
        result.put("partOfSpeech", hit.partOfSpeech());
        result.put("example", example);
        return result;
    }

    private String joinExample(JsonNode node, String prefix, String term, String suffix) {
        String value = node.path(prefix).asText("")
                + node.path(term).asText("")
                + node.path(suffix).asText("");
        value = value.replaceAll("\\s+", " ").trim();
        return value.isBlank() ? null : value;
    }

    private static String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) return "en";
        String normalized = language.trim().toLowerCase();
        int separator = Math.max(normalized.indexOf('-'), normalized.indexOf('_'));
        return separator > 0 ? normalized.substring(0, separator) : normalized;
    }

    private static String normalizePartOfSpeech(String value) {
        if (value == null) return null;
        return switch (value.toUpperCase()) {
            case "NOUN" -> "n.";
            case "VERB" -> "v.";
            case "ADJECTIVE", "ADJ" -> "adj.";
            case "ADVERB", "ADV" -> "adv.";
            case "PRONOUN" -> "pron.";
            case "PREPOSITION", "PREP" -> "prep.";
            case "CONJUNCTION", "CONJ" -> "conj.";
            case "INTERJECTION", "INTERJ" -> "interj.";
            default -> value.toLowerCase();
        };
    }

    private static String textOrNull(JsonNode node, String field) {
        String value = node.path(field).asText("").trim();
        return value.isBlank() ? null : value;
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    record DictionaryHit(String normalizedSource, String normalizedTarget,
                         String displayTarget, String partOfSpeech) {}
}
