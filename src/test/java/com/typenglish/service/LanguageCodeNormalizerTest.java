package com.typenglish.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguageCodeNormalizerTest {
    @Test
    void normalizesNamesThatAnAiMayUseIntoStoredLanguageCodes() {
        assertEquals("en", LanguageCodeNormalizer.normalize("英语"));
        assertEquals("en", LanguageCodeNormalizer.normalize("English"));
        assertEquals("ja", LanguageCodeNormalizer.normalize("日语"));
        assertEquals("de", LanguageCodeNormalizer.normalize("German"));
    }

    @Test
    void fallsBackToEnglishForUnknownOrBlankValues() {
        assertEquals("en", LanguageCodeNormalizer.normalize(null));
        assertEquals("en", LanguageCodeNormalizer.normalize(""));
        assertEquals("en", LanguageCodeNormalizer.normalize("英语学习"));
    }
}
