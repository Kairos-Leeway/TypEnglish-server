package com.typenglish.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class AzureSpeechProvider implements SpeechProvider {
    private static final String EDGE_TTS_URL =
            "https://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1";
    private static final String OUTPUT_FORMAT = "audio-24khz-48kbitrate-mono-mp3";
    private static final String RATE = "-18%";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${tts.azure.key:}") private String azureKey;
    @Value("${tts.azure.region:eastus}") private String azureRegion;

    @Override
    public byte[] synthesize(String text, String lang) throws Exception {
        VoiceProfile profile = voiceFor(lang);
        String ssml = String.format(
                "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='%s'>" +
                "<voice name='%s'><prosody rate='%s'>%s</prosody></voice></speak>",
                profile.locale(), profile.voice(), RATE, escapeXml(text));

        boolean officialAzure = azureKey != null && !azureKey.isBlank();
        URI endpoint = officialAzure
                ? URI.create("https://" + azureRegion + ".tts.speech.microsoft.com/cognitiveservices/v1")
                : URI.create(EDGE_TTS_URL + "?TrustedClientToken=6A5AA1D4EAFF4E9FB37E23D68491D6F4");
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(endpoint)
                .header("Content-Type", "application/ssml+xml")
                .header("X-Microsoft-OutputFormat", OUTPUT_FORMAT)
                .header("User-Agent", "TypEnglish")
                .POST(HttpRequest.BodyPublishers.ofString(ssml));
        if (officialAzure) builder.header("Ocp-Apim-Subscription-Key", azureKey);

        HttpResponse<byte[]> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Speech service returned HTTP " + response.statusCode());
        }
        return response.body();
    }

    @Override
    public String profileId(String lang) {
        VoiceProfile profile = voiceFor(lang);
        return profile.voice() + "|rate=" + RATE + "|format=" + OUTPUT_FORMAT;
    }

    private VoiceProfile voiceFor(String lang) {
        String normalized = lang == null ? "en-US" : lang.toLowerCase();
        if (normalized.startsWith("zh")) return new VoiceProfile("zh-CN", "zh-CN-XiaoxiaoNeural");
        if (normalized.startsWith("ja")) return new VoiceProfile("ja-JP", "ja-JP-NanamiNeural");
        if (normalized.startsWith("de")) return new VoiceProfile("de-DE", "de-DE-KatjaNeural");
        return new VoiceProfile("en-US", "en-US-AvaMultilingualNeural");
    }

    private String escapeXml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private record VoiceProfile(String locale, String voice) {}
}
