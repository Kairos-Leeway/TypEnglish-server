package com.typenglish.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.typenglish.entity.AiConversation;
import com.typenglish.entity.AiConversationMessage;
import com.typenglish.common.BusinessException;
import com.typenglish.mapper.AiConversationMapper;
import com.typenglish.mapper.AiConversationMessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ConversationService {

    private final AiConversationMapper conversationMapper;
    private final AiConversationMessageMapper messageMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String MESSAGE_ENVELOPE_PREFIX = "<!--typenglish-message-v1-->";

    public ConversationService(AiConversationMapper conversationMapper,
                               AiConversationMessageMapper messageMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    /** 创建新对话 */
    @Transactional
    public AiConversation createConversation(Long userId, String title) {
        AiConversation conv = new AiConversation();
        conv.setUserId(userId);
        conv.setCreateTime(LocalDateTime.now());
        conv.setUpdateTime(LocalDateTime.now());
        conv.setTitle(title != null && !title.isBlank() ? title : "新对话");
        conversationMapper.insert(conv);
        return conv;
    }

    /** 更新对话标题 */
    public void updateTitle(Long conversationId, Long userId, String title) {
        AiConversation conv = requireOwnedConversation(conversationId, userId);
        conv.setTitle(title);
        conv.setUpdateTime(LocalDateTime.now());
        conversationMapper.updateById(conv);
    }

    /** 保存消息 */
    public void saveMessage(Long conversationId, Long userId, String role, String content) {
        saveMessage(conversationId, userId, role, content, List.of());
    }

    public void saveMessage(Long conversationId, Long userId, String role, String content,
                            List<Map<String, Object>> tools) {
        requireOwnedConversation(conversationId, userId);
        AiConversationMessage msg = new AiConversationMessage();
        msg.setConversationId(conversationId);
        msg.setUserId(userId);
        msg.setRole(role);
        msg.setContent(encodeContent(content, tools));
        msg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(msg);
        // 更新会话时间
        AiConversation conv = new AiConversation();
        conv.setId(conversationId);
        conv.setUpdateTime(LocalDateTime.now());
        conversationMapper.updateById(conv);
    }

    /** 获取用户对话列表（按更新时间倒序） */
    public List<AiConversation> getConversations(Long userId) {
        return conversationMapper.selectList(
                new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getUserId, userId)
                        .orderByDesc(AiConversation::getUpdateTime));
    }

    /** 获取对话消息历史 */
    public List<AiConversationMessage> getMessages(Long conversationId, Long userId) {
        requireOwnedConversation(conversationId, userId);
        List<AiConversationMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<AiConversationMessage>()
                        .eq(AiConversationMessage::getConversationId, conversationId)
                        .eq(AiConversationMessage::getUserId, userId)
                        .orderByAsc(AiConversationMessage::getCreateTime));
        messages.forEach(this::decodeContent);
        return messages;
    }

    /** 删除对话及其消息 */
    @Transactional
    public void deleteConversation(Long conversationId, Long userId) {
        requireOwnedConversation(conversationId, userId);
        messageMapper.delete(new LambdaQueryWrapper<AiConversationMessage>()
                .eq(AiConversationMessage::getConversationId, conversationId)
                .eq(AiConversationMessage::getUserId, userId));
        conversationMapper.delete(new LambdaQueryWrapper<AiConversation>()
                .eq(AiConversation::getId, conversationId)
                .eq(AiConversation::getUserId, userId));
    }

    public AiConversation requireOwnedConversation(Long conversationId, Long userId) {
        AiConversation conversation = conversationMapper.selectOne(
                new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getId, conversationId)
                        .eq(AiConversation::getUserId, userId));
        if (conversation == null) {
            throw new BusinessException(404, "对话不存在");
        }
        return conversation;
    }

    private String encodeContent(String content, List<Map<String, Object>> tools) {
        if (tools == null || tools.isEmpty()) return content;
        try {
            return MESSAGE_ENVELOPE_PREFIX + objectMapper.writeValueAsString(Map.of(
                    "content", content,
                    "tools", tools));
        } catch (Exception ignored) {
            return content;
        }
    }

    private void decodeContent(AiConversationMessage message) {
        String stored = message.getContent();
        if (stored == null || !stored.startsWith(MESSAGE_ENVELOPE_PREFIX)) {
            message.setTools(List.of());
            return;
        }
        try {
            Map<String, Object> envelope = objectMapper.readValue(
                    stored.substring(MESSAGE_ENVELOPE_PREFIX.length()), new TypeReference<>() {});
            message.setContent(String.valueOf(envelope.getOrDefault("content", "")));
            Object rawTools = envelope.get("tools");
            if (rawTools instanceof List<?> list) {
                @SuppressWarnings("unchecked") List<Map<String, Object>> tools =
                        (List<Map<String, Object>>) (List<?>) list;
                message.setTools(tools);
            } else {
                message.setTools(List.of());
            }
        } catch (Exception ignored) {
            message.setContent(stored);
            message.setTools(List.of());
        }
    }
}
