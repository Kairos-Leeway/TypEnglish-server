package com.typenglish.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sentence_error")
public class SentenceError {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long sentenceId;
    private String english;
    private String chinese;
    private String mode;
    /** JSON: [{"index":0,"word":"Hello","correct":true,"answer":"Hello"},...] */
    private String slotResults;
    private Integer totalSlots;
    private Integer correctSlots;
    private Boolean mastered;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
