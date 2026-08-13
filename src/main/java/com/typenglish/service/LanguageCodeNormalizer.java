package com.typenglish.service;

import java.util.Locale;
import java.util.Map;

/** 将 AI 或前端可能使用的语言名称收敛为词库中的语言代码。 */
public final class LanguageCodeNormalizer {
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("en", "en"), Map.entry("en-us", "en"), Map.entry("english", "en"), Map.entry("英语", "en"),
            Map.entry("ja", "ja"), Map.entry("ja-jp", "ja"), Map.entry("japanese", "ja"), Map.entry("日语", "ja"),
            Map.entry("de", "de"), Map.entry("de-de", "de"), Map.entry("german", "de"), Map.entry("德语", "de")
    );

    private LanguageCodeNormalizer() {}

    public static String normalize(String value) {
        if (value == null || value.isBlank()) return "en";
        String key = value.trim().toLowerCase(Locale.ROOT);
        return ALIASES.getOrDefault(key, "en");
    }
}
