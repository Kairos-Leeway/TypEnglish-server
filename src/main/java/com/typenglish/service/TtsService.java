package com.typenglish.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TtsService {
    private final SpeechProvider speechProvider;
    private final TtsAudioCache audioCache;
    private final ConcurrentHashMap<String, Object> keyLocks = new ConcurrentHashMap<>();

    public TtsService(SpeechProvider speechProvider, TtsAudioCache audioCache) {
        this.speechProvider = speechProvider;
        this.audioCache = audioCache;
    }

    public SpeechResult speak(String text, String lang) throws Exception {
        String normalizedText = text == null ? "" : text.trim().replaceAll("\\s+", " ");
        if (normalizedText.isEmpty()) throw new IllegalArgumentException("text must not be blank");
        String normalizedLang = lang == null || lang.isBlank() ? "en-US" : lang;
        String cacheKey = cacheKey(normalizedText, normalizedLang, speechProvider.profileId(normalizedLang));

        var cached = audioCache.get(cacheKey);
        if (cached.isPresent()) return new SpeechResult(cached.get(), CacheStatus.HIT);

        Object lock = keyLocks.computeIfAbsent(cacheKey, ignored -> new Object());
        try {
            synchronized (lock) {
                cached = audioCache.get(cacheKey);
                if (cached.isPresent()) return new SpeechResult(cached.get(), CacheStatus.HIT);

                byte[] audio = speechProvider.synthesize(normalizedText, normalizedLang);
                audioCache.put(cacheKey, audio);
                return new SpeechResult(audio, audioCache.isEnabled() ? CacheStatus.MISS : CacheStatus.BYPASS);
            }
        } finally {
            keyLocks.remove(cacheKey, lock);
        }
    }

    static String cacheKey(String text, String lang, String profileId) throws Exception {
        String source = "tts-v1\n" + lang.toLowerCase() + "\n" + profileId + "\n" + text;
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
        return "audio/" + HexFormat.of().formatHex(digest) + ".mp3";
    }

    public enum CacheStatus { HIT, MISS, BYPASS }
    public record SpeechResult(byte[] audio, CacheStatus cacheStatus) {}
}
