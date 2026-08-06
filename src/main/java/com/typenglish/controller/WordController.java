package com.typenglish.controller;

import com.typenglish.common.PageResult;
import com.typenglish.common.Result;
import com.typenglish.entity.WordBank;
import com.typenglish.service.WordService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/words")
public class WordController {

    private final WordService wordService;

    public WordController(WordService wordService) {
        this.wordService = wordService;
    }

    @GetMapping
    public Result<PageResult<WordBank>> list(
            @RequestParam(defaultValue = "en") String language,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer difficulty) {
        return Result.ok(wordService.list(language, page, size, difficulty));
    }

    @GetMapping("/lookup")
    public Result<Map<String, Object>> lookup(
            @RequestParam(defaultValue = "en") String language,
            @RequestParam String word) {
        return Result.ok(wordService.lookup(language, word));
    }

    @PostMapping("/import")
    public Result<Map<String, Object>> importWords(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> words = (List<Map<String, Object>>) body.get("words");
        String language = (String) body.getOrDefault("language", "en");
        var result = wordService.importWords(words, language);
        return Result.ok(Map.of("created", result.created(), "skipped", result.skipped()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteWord(@PathVariable Long id) {
        wordService.deleteById(id);
        return Result.ok();
    }

    @DeleteMapping("/category")
    public Result<Map<String, Object>> deleteByCategory(@RequestBody Map<String, String> body) {
        String category = body.get("category");
        String language = body.getOrDefault("language", "en");
        long count = wordService.deleteByCategory(category, language);
        return Result.ok(Map.of("deleted", count));
    }
}
