package com.typenglish.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("word_bank")
public class WordBank {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String language;
    private String category;
    private String word;
    private String phonetic;
    private String translation;
    private String partOfSpeech;
    private String example;
    private Integer difficulty;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
