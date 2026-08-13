package com.typenglish.controller;

import com.typenglish.common.Result;
import com.typenglish.entity.AiConversation;
import com.typenglish.entity.AiConversationMessage;
import com.typenglish.service.AiChatService;
import com.typenglish.service.AiToolService;
import com.typenglish.service.ConversationService;
import com.typenglish.security.JwtInterceptor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final AiChatService aiChatService;
    private final AiToolService aiToolService;
    private final ConversationService conversationService;

    public AiChatController(AiChatService aiChatService, AiToolService aiToolService,
                            ConversationService conversationService) {
        this.aiChatService = aiChatService;
        this.aiToolService = aiToolService;
        this.conversationService = conversationService;
    }

    private Long currentUserId() {
        Long uid = JwtInterceptor.CURRENT_USER.get();
        if (uid == null) throw new RuntimeException("未登录");
        return uid;
    }

    // ──────────── 对话管理 API ────────────

    /** 获取用户对话列表 */
    @GetMapping("/conversations")
    public Result<List<AiConversation>> getConversations() {
        return Result.ok(conversationService.getConversations(currentUserId()));
    }

    /** 获取对话历史消息 */
    @GetMapping("/conversation/{id}/messages")
    public Result<List<AiConversationMessage>> getMessages(@PathVariable Long id) {
        return Result.ok(conversationService.getMessages(id, currentUserId()));
    }

    /** 删除对话 */
    @DeleteMapping("/conversation/{id}")
    public Result<Void> deleteConversation(@PathVariable Long id) {
        conversationService.deleteConversation(id, currentUserId());
        return Result.ok();
    }

    /** 修改对话标题 */
    @PutMapping("/conversation/{id}/title")
    public Result<Void> updateTitle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String title = body.get("title");
        if (title != null && !title.isBlank()) {
            conversationService.updateTitle(id, currentUserId(), title);
        }
        return Result.ok();
    }

    // ──────────── 非流式聊天 ────────────

    @PostMapping("/chat")
    public Result<Map<String, String>> chat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        if (message.isBlank()) return Result.ok(Map.of("reply", "请告诉我你想聊什么?"));
        return Result.ok(Map.of("reply", aiChatService.chat(currentUserId(), message)));
    }

    // ──────────── 流式 SSE 对话（带对话持久化） ────────────

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        if (message.isBlank()) {
            SseEmitter bad = new SseEmitter();
            bad.completeWithError(new IllegalArgumentException("消息不能为空"));
            return bad;
        }

        Long userId = currentUserId();
        return aiChatService.streamChat(message, userId, body.get("conversationId"));
    }

    // ──────────── AI 出题 ────────────

    @PostMapping("/generate-words")
    public Result<Map<String, Object>> generateWords(@RequestBody Map<String, Object> body) {
        String topic = (String) body.getOrDefault("topic", "日常");
        int count = body.get("count") instanceof Number n ? n.intValue() : 10;
        int diff = body.get("difficulty") instanceof Number n ? n.intValue() : 2;
        String lang = (String) body.getOrDefault("language", "en");
        return Result.ok(aiToolService.doGenerateWords(topic, count, diff, lang));
    }

    @PostMapping("/generate-sentences")
    public Result<Map<String, Object>> generateSentences(@RequestBody Map<String, Object> body) {
        String topic = (String) body.getOrDefault("topic", "日常");
        int count = body.get("count") instanceof Number n ? n.intValue() : 5;
        int diff = body.get("difficulty") instanceof Number n ? n.intValue() : 2;
        String lang = (String) body.getOrDefault("language", "en");
        return Result.ok(aiToolService.doGenerateSentencesBatch(topic, count, diff, lang));
    }

    /**
     * SSE 分批生成句子（上限 100），每批 15 条，实时推送进度。
     * 小批量（≤15）走此接口也能拿到进度反馈。
     */
    @PostMapping(value = "/generate-sentences-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateSentencesStream(@RequestBody Map<String, Object> body) {
        String topic = (String) body.getOrDefault("topic", "日常");
        int count = body.get("count") instanceof Number n ? n.intValue() : 30;
        int diff = body.get("difficulty") instanceof Number n ? n.intValue() : 2;
        String lang = (String) body.getOrDefault("language", "en");
        return aiToolService.generateSentencesStream(topic, count, diff, lang);
    }
}
