package com.typenglish.dto;

import lombok.Data;
import java.util.List;

public class PracticeDTO {

    @Data
    public static class SessionReq {
        private String language = "en";
        private String mode = "typing";
        private String category;
        private int count = 10;
    }

    @Data
    public static class SubmitReq {
        private Long wordId;
        private String wordText;
        private String language;
        private String mode;
        private Boolean correct;
        private String answer;
        private int attempts = 1;
        private boolean hintUsed;
        private boolean skipped;
    }

    @Data
    public static class StatsResp {
        private long total;
        private long correct;
        private int accuracy;
        private List<ModeCount> byMode;
    }

    @Data
    public static class ModeCount {
        private String mode;
        private long count;
    }
}
