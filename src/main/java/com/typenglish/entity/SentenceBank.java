package com.typenglish.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sentence_bank")
public class SentenceBank {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String language;
    private String chinese;
    private String english;
    private Integer difficulty;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
