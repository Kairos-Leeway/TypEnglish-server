package com.typenglish.service;

import java.util.Optional;

public interface TtsAudioCache {
    Optional<byte[]> get(String key);
    void put(String key, byte[] audio);
    boolean isEnabled();
}
