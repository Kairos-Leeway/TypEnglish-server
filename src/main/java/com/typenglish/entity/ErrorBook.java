package com.typenglish.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("error_book")
public class ErrorBook {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long wordId;
    private Integer errorCount;
    private LocalDateTime lastErrorAt;
    private LocalDateTime nextReviewAt;
    private Boolean mastered;

    // SM-2 算法字段
    private Double easinessFactor;  // 难易度因子，默认 2.5，最低 1.3
    private Integer reviewInterval; // 当前复习间隔（天）
    private Integer repetitions;    // 连续答对次数
}
