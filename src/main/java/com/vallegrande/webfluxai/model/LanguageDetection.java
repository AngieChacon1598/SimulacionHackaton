package com.vallegrande.webfluxai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "language_detections")
public class LanguageDetection {
    @Id
    private String id;
    private String text;
    private DetectedLanguage detectedLanguage;
    private Double confidence;
    private LocalDateTime createdAt;
    private String apiVersion;
    private LocalDateTime updatedAt;
    private Boolean deleted;
    private LocalDateTime deletedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetectedLanguage {
        private String code;
        private String name;
    }
}
