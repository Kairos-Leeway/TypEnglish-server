package com.typenglish.service;

import com.typenglish.entity.AiConversationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class DatabaseBackedChatMemory implements ChatMemory {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBackedChatMemory.class);

    private static final int MAX_MESSAGES = 100;

    private final MessageWindowChatMemory delegate;
    private final ConversationService conversationService;

    private final Set<String> loadedConversations = ConcurrentHashMap.newKeySet();

    public DatabaseBackedChatMemory(ConversationService conversationService) {
        this.conversationService = conversationService;
        this.delegate = MessageWindowChatMemory.builder()
                .maxMessages(MAX_MESSAGES)
                .build();
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        delegate.add(conversationId, messages);
    }

    @Override
    public List<Message> get(String conversationId) {
        ensureLoaded(conversationId);
        return delegate.get(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        delegate.clear(conversationId);
        loadedConversations.remove(conversationId);
        log.debug("已清除会话 {} 的记忆", conversationId);
    }

    public void delete(String conversationId) {
        conversationService.deleteConversation(Long.parseLong(conversationId));
        clear(conversationId);
        log.info("已彻底删除会话 {} 的所有历史记录", conversationId);
    }

    private void ensureLoaded(String conversationId) {
        if (loadedConversations.contains(conversationId)) {
            return;
        }

        List<AiConversationMessage> history = conversationService.getMessages(Long.parseLong(conversationId));
        if (history == null || history.isEmpty()) {
            loadedConversations.add(conversationId);
            return;
        }

        List<AiConversationMessage> recent;
        if (history.size() > MAX_MESSAGES) {
            recent = history.subList(history.size() - MAX_MESSAGES, history.size());
            log.info("会话 {} 历史消息 {} 条，取最近 {} 条加载到记忆窗口",
                    conversationId, history.size(), MAX_MESSAGES);
        } else {
            recent = history;
        }

        List<Message> messages = recent.stream()
                .map(this::toMessage)
                .collect(Collectors.toList());

        delegate.add(conversationId, messages);
        loadedConversations.add(conversationId);

        log.debug("已从 DB 加载会话 {} 的 {} 条消息到记忆窗口", conversationId, messages.size());
    }

    private Message toMessage(AiConversationMessage msg) {
        String content = msg.getContent();
        if ("user".equals(msg.getRole())) {
            return new UserMessage(content);
        } else {
            return new AssistantMessage(content);
        }
    }
}
