package com.typenglish.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class AzureTranslatorServiceTest {
    private final AzureTranslatorService service = new AzureTranslatorService(
            null, new ObjectMapper(), "key", "eastasia", "https://example.test");

    @Test
    void parsesHighestConfidenceDictionaryTranslationAndPartOfSpeech() throws Exception {
        String json = """
                [{
                  "normalizedSource":"quiet",
                  "displaySource":"quiet",
                  "translations":[
                    {"normalizedTarget":"安静的","displayTarget":"安静的","posTag":"ADJECTIVE","confidence":0.91},
                    {"normalizedTarget":"寂静","displayTarget":"寂静","posTag":"NOUN","confidence":0.42}
                  ]
                }]
                """;

        AzureTranslatorService.DictionaryHit hit = service.parseDictionaryResponse(json);

        assertNotNull(hit);
        assertEquals("quiet", hit.normalizedSource());
        assertEquals("安静的", hit.normalizedTarget());
        assertEquals("安静的", hit.displayTarget());
        assertEquals("adj.", hit.partOfSpeech());
    }

    @Test
    void parsesBilingualDictionaryExample() throws Exception {
        String json = """
                [{"examples":[{
                  "sourcePrefix":"a ","sourceTerm":"quiet","sourceSuffix":" room",
                  "targetPrefix":"一个","targetTerm":"安静的","targetSuffix":"房间"
                }]}]
                """;

        assertEquals("a quiet room (一个安静的房间)", service.parseExampleResponse(json));
    }

    @Test
    void parsesPlainTranslationFallback() throws Exception {
        String json = """
                [{"translations":[{"text":"安静的","to":"zh-Hans"}]}]
                """;

        assertEquals("安静的", service.parseTranslationResponse(json));
    }

    @Test
    void blankKeyDisablesAzureProvider() {
        AzureTranslatorService disabled = new AzureTranslatorService(
                null, new ObjectMapper(), "  ", "eastasia", "https://example.test");

        assertFalse(disabled.isConfigured());
        assertNull(disabled.lookup("quiet", "en"));
    }

    @Test
    void springCanInstantiateTheProductionConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(AzureTranslatorService.class);

            assertDoesNotThrow(context::refresh);
            assertNotNull(context.getBean(AzureTranslatorService.class));
        }
    }
}
