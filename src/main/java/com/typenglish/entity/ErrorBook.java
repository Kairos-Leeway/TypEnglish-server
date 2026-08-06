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
}
