package com.typenglish.controller;

import com.typenglish.common.Result;
import com.typenglish.service.AiChatService;
import com.typenglish.service.AiToolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);
    private final AiChatService aiChatService;
    private final AiToolService aiToolService;
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

    public AiChatController(AiChatService aiChatService, AiToolService aiToolService) {
        this.aiChatService = aiChatService;
        this.aiToolService = aiToolService;
    }

    @PostMapping("/chat")
    public Result<Map<String, String>> chat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        if (message.isBlank()) return Result.ok(Map.of("reply", "请告诉我你想聊什么?"));
        return Result.ok(Map.of("reply", aiChatService.chat(message)));
    }

    /** 流式 SSE 对话 */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        if (message.isBlank()) {
            SseEmitter bad = new SseEmitter();
            bad.completeWithError(new IllegalArgumentException("消息不能为空"));
            return bad;
        }

        SseEmitter emitter = new SseEmitter(300_000L);

        streamExecutor.execute(() -> {
            try {
                var flux = aiChatService.stream(message);
                flux.doOnNext(token -> {
                    try {
                        emitter.send(SseEmitter.event().name("message").data(token, MediaType.TEXT_PLAIN));
                    } catch (IOException e) {
                        throw new RuntimeException("SSE send error", e);
                    }
                }).doOnComplete(() -> {
                    try {
                        emitter.send(SseEmitter.event().name("done").data("{\"status\":\"completed\"}"));
                        emitter.complete();
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                }).doOnError(err -> {
                    log.error("Stream error: {}", err.getMessage());
                    emitter.completeWithError(err);
                }).subscribe();
            } catch (Exception e) {
                log.error("SSE setup error: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @PostMapping("/generate-words")
    public Result<Map<String, Object>> generateWords(@RequestBody Map<String, Object> body) {
        String topic = (String) body.getOrDefault("topic", "日常");
        int count = body.get("count") instanceof Number n ? n.intValue() : 10;
        int diff = body.get("difficulty") instanceof Number n ? n.intValue() : 2;
        String lang = (String) body.getOrDefault("language", "en");
        return Result.ok(aiToolService.generateWords(topic, count, diff, lang));
    }

    @PostMapping("/generate-sentences")
    public Result<Map<String, Object>> generateSentences(@RequestBody Map<String, Object> body) {
        String topic = (String) body.getOrDefault("topic", "日常");
        int count = body.get("count") instanceof Number n ? n.intValue() : 5;
        int diff = body.get("difficulty") instanceof Number n ? n.intValue() : 2;
        String lang = (String) body.getOrDefault("language", "en");
        return Result.ok(aiToolService.generateSentences(topic, count, diff, lang));
    }
}
