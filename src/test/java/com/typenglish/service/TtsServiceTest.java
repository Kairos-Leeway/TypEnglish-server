package com.typenglish.service;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TtsServiceTest {
    @Test
    void repeatedTextIsSynthesizedOnceAndThenReadFromPersistentCache() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        SpeechProvider provider = new SpeechProvider() {
            public byte[] synthesize(String text, String lang) {
                calls.incrementAndGet();
                return (lang + ":" + text).getBytes();
            }
            public String profileId(String lang) { return "test-voice|rate=-18%|mp3"; }
        };
        Map<String, byte[]> objects = new HashMap<>();
        TtsAudioCache cache = new TtsAudioCache() {
            public Optional<byte[]> get(String key) { return Optional.ofNullable(objects.get(key)); }
            public void put(String key, byte[] audio) { objects.put(key, audio); }
            public boolean isEnabled() { return true; }
        };
        TtsService service = new TtsService(provider, cache);

        var first = service.speak("  Before   sleeping  ", "en-US");
        var second = service.speak("Before sleeping", "en-US");

        assertEquals(TtsService.CacheStatus.MISS, first.cacheStatus());
        assertEquals(TtsService.CacheStatus.HIT, second.cacheStatus());
        assertArrayEquals(first.audio(), second.audio());
        assertEquals(1, calls.get());
        assertEquals(1, objects.size());
    }

    @Test
    void voiceProfileChangesProduceDifferentCacheKeys() throws Exception {
        assertNotEquals(
                TtsService.cacheKey("hello", "en-US", "voice-a"),
                TtsService.cacheKey("hello", "en-US", "voice-b"));
    }
}
