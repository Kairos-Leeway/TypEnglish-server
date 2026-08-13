package com.typenglish.service;

public interface SpeechProvider {
    byte[] synthesize(String text, String lang) throws Exception;
    String profileId(String lang);
}
