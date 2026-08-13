package com.typenglish.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.Optional;

@Component
public class MinioTtsAudioCache implements TtsAudioCache {
    private static final Logger log = LoggerFactory.getLogger(MinioTtsAudioCache.class);

    private final String bucket;
    private final MinioClient client;
    private volatile boolean bucketReady;

    public MinioTtsAudioCache(
            @Value("${tts.cache.minio.endpoint:http://localhost:9000}") String endpoint,
            @Value("${tts.cache.minio.access-key:}") String accessKey,
            @Value("${tts.cache.minio.secret-key:}") String secretKey,
            @Value("${tts.cache.minio.bucket:lingua-learn-tts}") String bucket) {
        this.bucket = bucket;
        this.client = accessKey.isBlank() || secretKey.isBlank() ? null
                : MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
        if (client == null) {
            log.warn("TTS MinIO cache DISABLED: MINIO_ACCESS_KEY or MINIO_SECRET_KEY is missing");
        } else {
            log.info("TTS MinIO cache ENABLED: endpoint={}, bucket={}", endpoint, bucket);
        }
    }

    @Override
    public Optional<byte[]> get(String key) {
        if (client == null) return Optional.empty();
        try {
            ensureBucket();
            try (var stream = client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
                return Optional.of(stream.readAllBytes());
            }
        } catch (Exception e) {
            log.debug("TTS cache miss or MinIO unavailable for {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, byte[] audio) {
        if (client == null) return;
        try {
            ensureBucket();
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                    .stream(new ByteArrayInputStream(audio), (long) audio.length, -1L)
                    .contentType("audio/mpeg").build());
        } catch (Exception e) {
            log.warn("Could not persist TTS audio to MinIO: {}", e.getMessage());
        }
    }

    @Override
    public boolean isEnabled() {
        return client != null;
    }

    private synchronized void ensureBucket() throws Exception {
        if (bucketReady) return;
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
        bucketReady = true;
    }
}
