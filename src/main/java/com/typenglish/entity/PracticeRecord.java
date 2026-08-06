package com.typenglish.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("practice_record")
public class PracticeRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long wordId;
    private String mode;
    private Boolean correct;
    private String answer;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
