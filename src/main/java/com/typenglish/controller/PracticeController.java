package com.typenglish.controller;

import com.typenglish.common.Result;
import com.typenglish.dto.PracticeDTO;
import com.typenglish.service.PracticeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/practice")
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    @PostMapping("/session")
    public Result<Map<String, Object>> createSession(@RequestBody PracticeDTO.SessionReq req) {
        var result = practiceService.createSession(req.getLanguage(), req.getCount(), req.getCategory());
        return Result.ok(Map.of(
                "mode", req.getMode(),
                "words", result.words(),
                "fromErrorBook", result.fromErrorBook(),
                "fromBank", result.fromBank()
        ));
    }

    @PostMapping("/submit")
    public Result<Void> submit(@RequestBody PracticeDTO.SubmitReq req) {
        practiceService.submitAnswer(req.getWordId(), req.getMode(), req.getCorrect(),
                req.getAnswer(), req.getWordText(), req.getLanguage());
        return Result.ok();
    }

    @PostMapping("/submit-batch")
    public Result<Map<String, Object>> submitBatch(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        String mode = (String) body.getOrDefault("mode", "cloze");
        String language = (String) body.getOrDefault("language", "en");
        int recorded = 0, skipped = 0;
        for (Map<String, Object> item : items) {
            Boolean correct = (Boolean) item.get("correct");
            String answer = (String) item.get("answer");
            Object wordIdObj = item.get("wordId");
            String wordText = (String) item.get("wordText");
            Long wordId = wordIdObj instanceof Number n ? n.longValue() : null;
            try {
                practiceService.submitAnswer(wordId, mode, correct, answer, wordText, language);
                recorded++;
            } catch (Exception e) { skipped++; }
        }
        return Result.ok(Map.of("recorded", recorded, "skipped", skipped));
    }

    @PostMapping("/sentence-error")
    public Result<Map<String, Object>> recordSentenceError(@RequestBody Map<String, Object> body) {
        var result = practiceService.recordSentenceError(body);
        return Result.ok(Map.of("id", result.id()));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(practiceService.getStats());
    }
}
