package com.typenglish.service;

import java.util.Map;

public record AiToolExecutionEvent(String id, String phase, String name, String title,
                                   String summary, Map<String, Object> action) {}
