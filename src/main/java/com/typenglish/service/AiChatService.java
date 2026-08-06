package com.typenglish.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AiChatService {

    private final ChatClient chatClient;
    private final AiToolService toolService;
    private final DatabaseBackedChatMemory chatMemory;

    public AiChatService(OpenAiChatModel chatModel, AiToolService toolService,
                         DatabaseBackedChatMemory chatMemory) {
        this.toolService = toolService;
        this.chatMemory = chatMemory;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是 LinguaLearn 的 AI 学习助教。你可以调用以下工具:
                        - getMyErrorBook: 查询错题本
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

    /** 流式输出 — 带对话记忆管理 */
    public Flux<String> stream(String userMessage, Long userId, String conversationId) {
        AiToolService.currentStreamUserId = userId;
        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .stream()
                .content()
                .doFinally(s -> AiToolService.currentStreamUserId = null);
    }
}
