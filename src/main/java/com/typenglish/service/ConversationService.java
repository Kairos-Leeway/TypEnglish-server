package com.typenglish.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.typenglish.entity.AiConversation;
import com.typenglish.entity.AiConversationMessage;
import com.typenglish.mapper.AiConversationMapper;
import com.typenglish.mapper.AiConversationMessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConversationService {

    private final AiConversationMapper conversationMapper;
    private final AiConversationMessageMapper messageMapper;

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
    public void updateTitle(Long conversationId, String title) {
        AiConversation conv = new AiConversation();
        conv.setId(conversationId);
        conv.setTitle(title);
        conv.setUpdateTime(LocalDateTime.now());
        conversationMapper.updateById(conv);
    }

    /** 保存消息 */
    public void saveMessage(Long conversationId, Long userId, String role, String content) {
        AiConversationMessage msg = new AiConversationMessage();
        msg.setConversationId(conversationId);
        msg.setUserId(userId);
        msg.setRole(role);
        msg.setContent(content);
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
    public List<AiConversationMessage> getMessages(Long conversationId) {
        return messageMapper.selectList(
                new LambdaQueryWrapper<AiConversationMessage>()
                        .eq(AiConversationMessage::getConversationId, conversationId)
                        .orderByAsc(AiConversationMessage::getCreateTime));
    }

    /** 删除对话及其消息 */
    @Transactional
    public void deleteConversation(Long conversationId) {
        messageMapper.delete(new LambdaQueryWrapper<AiConversationMessage>()
                .eq(AiConversationMessage::getConversationId, conversationId));
        conversationMapper.deleteById(conversationId);
    }
}
