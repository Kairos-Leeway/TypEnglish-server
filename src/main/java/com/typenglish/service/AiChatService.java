package com.typenglish.service;

import com.typenglish.entity.AiConversation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Qualifier;

@Service
@Slf4j
public class AiChatService {

    private final ChatClient chatClient;
    private final AiToolService toolService;
    private final DatabaseBackedChatMemory chatMemory;
    private final ConversationService conversationService;
    private final Executor aiGenerateExecutor;

    public AiChatService(OpenAiChatModel chatModel, AiToolService toolService,
                         DatabaseBackedChatMemory chatMemory,
                         ConversationService conversationService,
                         @Qualifier("aiGenerateExecutor") Executor aiGenerateExecutor) {
        this.toolService = toolService;
        this.chatMemory = chatMemory;
        this.conversationService = conversationService;
        this.aiGenerateExecutor = aiGenerateExecutor;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是 LinguaLearn 的 AI 学习助教。你可以调用以下工具:
                        - getMyErrorBook: 查询单词错题本
                        - getMySentenceErrors: 查询句子错题本（完形填空和翻译的错句）
                        - getMyPracticeStats: 查询练习统计
                        - getWordDetail: 查询单词详情
                        - analyzeMyWeakPoints: 分析薄弱环节
                        - getMyRecentPractices: 查看近期练习
                        - generateWords: 生成单词并写入词库
                        - generateSentences: 生成句子并写入句库

                        始终用中文回复,保持友善鼓励的语气。支持 Markdown 格式。""")
                .defaultTools(toolService)
                .build();
    }

    public String chat(String userMessage) {
        return chatClient.prompt().user(userMessage).call().content();
    }

    /**
     * 流式 SSE 对话（会话持久化 + SSE 推送）。
     * 使用独立线程池执行 AI 调用，避免阻塞 NIO 线程。
     */
    public SseEmitter streamChat(String message, Long userId, String conversationIdStr) {
        SseEmitter emitter = new SseEmitter(300_000L);

        // 创建或复用对话
        Long conversationId;
        if (conversationIdStr != null && !conversationIdStr.isBlank()) {
            conversationId = Long.parseLong(conversationIdStr);
        } else {
            String title = message.length() > 30 ? message.substring(0, 30) : message;
            AiConversation conv = conversationService.createConversation(userId, title);
            conversationId = conv.getId();
        }

        // 保存用户消息
        conversationService.saveMessage(conversationId, userId, "user", message);

        final Long finalConvId = conversationId;

        aiGenerateExecutor.execute(() -> {
            StringBuilder fullReply = new StringBuilder();
            AtomicBoolean interrupted = new AtomicBoolean(false);

            try {
                AiToolService.currentStreamUserId = userId;
                chatClient.prompt()
                        .user(message)
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(finalConvId)))
                        .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                        .stream()
                        .content()
                        .toStream()
                        .forEach(token -> {
                            fullReply.append(token);
                            try {
                                emitter.send(SseEmitter.event().name("message").data(token, MediaType.TEXT_PLAIN));
                            } catch (IOException e) {
                                interrupted.set(true);
                                throw new RuntimeException("SSE send interrupt", e);
                            }
                        });
            } catch (Exception e) {
                if (interrupted.get()) {
                    log.info("Conversation {} stream interrupted, {} chars generated", finalConvId, fullReply.length());
                } else {
                    log.error("SSE stream error: {}", e.getMessage(), e);
                }
            } finally {
                // 保存完整回复
                if (fullReply.length() > 0) {
                    conversationService.saveMessage(finalConvId, userId, "assistant", fullReply.toString());
                }

                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("conversationId", finalConvId);
                meta.put("status", "completed");
                meta.put("interrupted", interrupted.get());
                meta.put("partialLength", fullReply.length());

                try {
                    emitter.send(SseEmitter.event().name("done").data(meta, MediaType.APPLICATION_JSON));
                } catch (IOException ignored) {}
                emitter.complete();
                AiToolService.currentStreamUserId = null;
            }
        });

        return emitter;
    }
}
