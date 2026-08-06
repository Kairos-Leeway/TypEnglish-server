package com.typenglish.controller;

import com.typenglish.common.Result;
import com.typenglish.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam(defaultValue = "en") String language) {
        return Result.ok(categoryService.listCategories(language));
    }
}
