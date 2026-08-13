package com.typenglish.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName("ai_conversation_message")
public class AiConversationMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private Long userId;
    private String role; // user / assistant
    private String content;
    /** 工具执行结果存放在 content 的兼容信封中，不新增数据库列。 */
    @TableField(exist = false)
    private List<Map<String, Object>> tools;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
