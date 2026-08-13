package com.typenglish.controller;

import com.typenglish.service.TtsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

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
        response.setHeader("Cache-Control", "private, max-age=86400");
        var result = ttsService.speak(text, lang);
        response.setHeader("X-TTS-Cache", result.cacheStatus().name());
        response.setContentLength(result.audio().length);
        response.getOutputStream().write(result.audio());
    }
}
