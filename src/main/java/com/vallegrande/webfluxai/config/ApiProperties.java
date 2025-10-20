package com.vallegrande.webfluxai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.apis")
public class ApiProperties {
    private final LanguageIdentify languageIdentify = new LanguageIdentify();
    private final JSearch jsearch = new JSearch();

    @Data
    public static class LanguageIdentify {
        private String baseUrl;
        private String apiKey;
        private String apiHost;
    }

    @Data
    public static class JSearch {
        private String baseUrl;
        private String apiKey;
        private String apiHost;
    }

    // Add this inner class to fix the compilation error
    @Data
    public static class LanguageIdentifyConfig {
        private String baseUrl;
        private String apiKey;
    }
}
