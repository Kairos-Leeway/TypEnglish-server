package com.typenglish.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_conversation_message")
public class AiConversationMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private Long userId;
    private String role; // user / assistant
    private String content;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
