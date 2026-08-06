package com.typenglish.service;

import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class TtsService {

    private static final String EDGE_TTS_URL =
            "https://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void speak(String text, String lang, OutputStream outputStream) throws Exception {
        String voice = lang.startsWith("en") ? "en-US-AriaNeural" :
                       lang.startsWith("ja") ? "ja-JP-NanamiNeural" :
                       lang.startsWith("de") ? "de-DE-KatjaNeural" : "en-US-AriaNeural";

        String ssml = String.format(
                "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='%s'>" +
                "<voice name='%s'><prosody rate='0.85'>%s</prosody></voice></speak>",
                lang, voice, escapeXml(text));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(EDGE_TTS_URL + "?TrustedClientToken=6A5AA1D4EAFF4E9FB37E23D68491D6F4"))
                .header("Content-Type", "application/ssml+xml")
                .header("User-Agent", "Mozilla/5.0")
                .POST(HttpRequest.BodyPublishers.ofString(ssml))
                .build();

        HttpResponse<byte[]> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        outputStream.write(resp.body());
    }

    private String escapeXml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;")
                   .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
