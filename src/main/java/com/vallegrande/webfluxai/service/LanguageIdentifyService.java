package com.vallegrande.webfluxai.service;

import com.vallegrande.webfluxai.config.ApiProperties;
import com.vallegrande.webfluxai.config.ApiProperties.LanguageIdentify;
import com.vallegrande.webfluxai.exception.ApiException;
import com.vallegrande.webfluxai.model.LanguageDetection;
import com.vallegrande.webfluxai.model.LanguageDetection.DetectedLanguage;
import com.vallegrande.webfluxai.repository.LanguageDetectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;

@Slf4j
@Service
@RequiredArgsConstructor
public class LanguageIdentifyService {

    @Qualifier("languageIdentifyWebClient")
    private final WebClient languageIdentifyWebClient;
    private final LanguageDetectionRepository repository;
    private final ApiProperties apiProperties;

    public Mono<JsonNode> detectLanguage(String text) {
        log.info("Detecting language for text: {}", text);
        
        LanguageIdentify config = apiProperties.getLanguageIdentify();
        if (config == null) {
            return Mono.error(new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "Language identification configuration is missing"
            ));
        }
        
        return languageIdentifyWebClient.post()
                .uri("/languageIdentify")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("text", text))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(error -> {
                                    log.error("Error detecting language: {}", error);
                                    return Mono.error(new ApiException(
                                            HttpStatus.valueOf(clientResponse.statusCode().value()),
                                            "Error detecting language: " + error
                                    ));
                                })
                )
                .bodyToMono(JsonNode.class)
                .flatMap(response -> {
                    JsonNode codes = response.get("languageCodes");
                    if (codes != null && codes.isArray() && codes.size() > 0) {
                        JsonNode primary = codes.get(0);
                        String languageCode = primary.get("code").asText();
                        double confidence = primary.get("confidence").asDouble();
                        
                        log.info("Idioma detectado: {} con confianza: {}", languageCode, confidence);
                        
                        LanguageDetection detection = LanguageDetection.builder()
                                .text(text)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .deleted(false)
                                .confidence(confidence)
                                .detectedLanguage(DetectedLanguage.builder()
                                        .code(languageCode)
                                        .name(getLanguageName(languageCode))
                                        .build())
                                .build();
                        
                        return repository.save(detection)
                                .map(savedDetection -> {
                                    log.info("Detección guardada en MongoDB con ID: {}", savedDetection.getId());
                                    return response;
                                });
                    }
                    
                    log.warn("No se encontraron códigos de idioma en la respuesta");
                    return Mono.just(response);
                })
                .doOnNext(result -> log.info("Resultado final: {}", result));
    }
    
    public Mono<LanguageDetection> getDetectionById(String id) {
        return repository.findByIdAndDeletedFalse(id)
                .switchIfEmpty(Mono.error(new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Language detection not found with id: " + id
                )));
    }

    public Flux<LanguageDetection> getAllDetections() {
        return repository.findByDeletedFalse();
    }

    public Flux<LanguageDetection> getDeletedDetections() {
        return repository.findByDeletedTrue();
    }

    public Flux<LanguageDetection> getAllDetectionsIncludingDeleted() {
        return repository.findAll();
    }

    public Mono<LanguageDetection> updateDetection(String id, String newText) {
        log.info("Updating language detection id: {} with new text", id);
        return getDetectionById(id)
                .flatMap(existing -> languageIdentifyWebClient.post()
                        .uri("/languageIdentify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("text", newText))
                        .retrieve()
                        .onStatus(
                                status -> status.is4xxClientError() || status.is5xxServerError(),
                                clientResponse -> clientResponse.bodyToMono(String.class)
                                        .flatMap(error -> {
                                            log.error("Error updating detection via API: {}", error);
                                            return Mono.error(new ApiException(
                                                    HttpStatus.valueOf(clientResponse.statusCode().value()),
                                                    "Error updating detection: " + error
                                            ));
                                        })
                        )
                        .bodyToMono(JsonNode.class)
                        .flatMap(response -> {
                            JsonNode codes = response.get("languageCodes");
                            if (codes != null && codes.isArray() && codes.size() > 0) {
                                JsonNode primary = codes.get(0);
                                String languageCode = primary.get("code").asText();
                                double confidence = primary.get("confidence").asDouble();
                                existing.setText(newText);
                                existing.setConfidence(confidence);
                                existing.setDetectedLanguage(DetectedLanguage.builder()
                                        .code(languageCode)
                                        .name(getLanguageName(languageCode))
                                        .build());
                                existing.setUpdatedAt(LocalDateTime.now());
                                return repository.save(existing);
                            }
                            return Mono.error(new ApiException(HttpStatus.BAD_GATEWAY, "Invalid response from LanguageIdentify API"));
                        }));
    }

    public Mono<Void> softDeleteDetection(String id) {
        log.info("Soft-deleting language detection id: {}", id);
        return getDetectionById(id)
                .flatMap(existing -> {
                    existing.setDeleted(true);
                    existing.setDeletedAt(LocalDateTime.now());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return repository.save(existing);
                })
                .then();
    }

    public Mono<LanguageDetection> restoreDetection(String id) {
        log.info("Restoring language detection id: {}", id);
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Language detection not found with id: " + id
                )))
                .flatMap(existing -> {
                    if (!Boolean.TRUE.equals(existing.getDeleted())) {
                        return Mono.error(new ApiException(
                                HttpStatus.BAD_REQUEST,
                                "Language detection is not deleted and cannot be restored"
                        ));
                    }
                    existing.setDeleted(false);
                    existing.setDeletedAt(null);
                    existing.setUpdatedAt(LocalDateTime.now());
                    return repository.save(existing);
                });
    }
    
    // 🔹 Nuevo método agregado
    public Mono<Void> permanentDeleteDetection(String id) {
        log.info("Permanently deleting language detection id: {}", id);
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Language detection not found with id: " + id
                )))
                .flatMap(detection -> repository.delete(detection));
    }
    
    private String getLanguageName(String code) {
        return switch (code.toLowerCase()) {
            case "en" -> "English";
            case "es" -> "Spanish";
            case "fr" -> "French";
            case "de" -> "German";
            case "it" -> "Italian";
            case "pt" -> "Portuguese";
            case "ru" -> "Russian";
            case "zh" -> "Chinese";
            case "ja" -> "Japanese";
            case "ko" -> "Korean";
            default -> "Unknown";
        };
    }
}
