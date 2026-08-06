package com.typenglish.controller;

import com.typenglish.common.PageResult;
import com.typenglish.common.Result;
import com.typenglish.security.JwtInterceptor;
import com.typenglish.service.ErrorBookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/errorbook")
public class ErrorBookController {

    private final ErrorBookService errorBookService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ErrorBookController(ErrorBookService errorBookService) {
        this.errorBookService = errorBookService;
    }

    @GetMapping
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(errorBookService.listWords(page, size));
    }

    @GetMapping("/sentences")
    public Result<PageResult<Map<String, Object>>> listSentences(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        return Result.ok(errorBookService.listSentences(page, size));
    }

    @DeleteMapping("/{id}")
    public Result<Void> markMastered(@PathVariable Long id) {
        errorBookService.markWordMastered(id);
        return Result.ok();
    }

    @DeleteMapping("/sentences/{id}")
    public Result<Void> markSentenceMastered(@PathVariable Long id) {
        errorBookService.markSentenceMastered(id);
        return Result.ok();
    }

    @DeleteMapping("/clear-all")
    public Result<Map<String, Object>> clearAllWords() {
        long count = errorBookService.clearAllWords();
        return Result.ok(Map.of("cleared", count));
    }

    @DeleteMapping("/sentences/clear-all")
    public Result<Map<String, Object>> clearAllSentences() {
        long count = errorBookService.clearAllSentences();
        return Result.ok(Map.of("cleared", count));
    }

    @PutMapping("/sentences/{id}")
    public Result<Map<String, Object>> updateSentenceError(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int correctSlots = body.get("correctSlots") instanceof Number n ? n.intValue() : 0;
        int totalSlots = body.get("totalSlots") instanceof Number n ? n.intValue() : 0;
        String slotResults;
        try {
            slotResults = objectMapper.writeValueAsString(body.get("slotResults"));
        } catch (Exception e) {
            slotResults = "[]";
        }
        boolean mastered = errorBookService.updateSentenceError(id, correctSlots, totalSlots, slotResults);
        return Result.ok(Map.of("mastered", mastered));
    }

    // ──────────── SM-2 复习接口 ────────────

    /**
     * 提交单词复习结果，自动计算 SM-2 间隔。
     * body: { "correct": true/false }
     * 不需要手动自评，系统根据 history 自动算 quality。
     */
    @PostMapping("/review/{errorBookId}")
    public Result<Map<String, Object>> reviewWord(@PathVariable Long errorBookId,
                                                   @RequestBody Map<String, Object> body) {
        boolean correct = Boolean.TRUE.equals(body.get("correct"));
        return Result.ok(errorBookService.autoReview(errorBookId, correct));
    }

    /** 获取到期待复习的错题 */
    @GetMapping("/due")
    public Result<java.util.List<Map<String, Object>>> getDueReviews() {
        Long userId = JwtInterceptor.CURRENT_USER.get();
        if (userId == null) return Result.fail(401, "未登录");
        return Result.ok(errorBookService.getDueReviews(userId));
    }
}
