package com.typenglish.controller;

import com.typenglish.service.TtsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;

@RestController
@RequestMapping("/api/tts")
public class TtsController {

    private final TtsService ttsService;

    public TtsController(TtsService ttsService) {
        this.ttsService = ttsService;
    }

    @GetMapping
    public void speak(@RequestParam String text,
                      @RequestParam(defaultValue = "en-US") String lang,
                      HttpServletResponse response) throws Exception {
        response.setContentType("audio/mpeg");
        response.setHeader("Cache-Control", "public, max-age=86400");
        try (OutputStream os = response.getOutputStream()) {
            ttsService.speak(text, lang, os);
        }
    }
}
