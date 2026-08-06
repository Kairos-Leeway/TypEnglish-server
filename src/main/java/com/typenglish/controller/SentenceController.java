package com.typenglish.controller;

import com.typenglish.common.Result;
import com.typenglish.service.SentenceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sentences")
public class SentenceController {

    private final SentenceService sentenceService;

    public SentenceController(SentenceService sentenceService) {
        this.sentenceService = sentenceService;
    }

    @GetMapping
    public Result<List<Map<String, Object>>> getSentences(
            @RequestParam(defaultValue = "en") String language,
            @RequestParam(defaultValue = "5") int count) {
        return Result.ok(sentenceService.getSentences(language, count));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteSentence(@PathVariable Long id) {
        sentenceService.deleteById(id);
        return Result.ok();
    }
}
