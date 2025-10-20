package com.vallegrande.webfluxai.controller;

import com.vallegrande.webfluxai.dto.LanguageDetectionRequest;
import com.vallegrande.webfluxai.model.LanguageDetection;
import com.vallegrande.webfluxai.service.LanguageIdentifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/v1/language")
@RequiredArgsConstructor
public class LanguageIdentifyController {

    private final LanguageIdentifyService languageIdentifyService;

    @PostMapping("/detect")
    public Mono<ResponseEntity<JsonNode>> detectLanguage(@RequestBody LanguageDetectionRequest request) {
        return languageIdentifyService.detectLanguage(request.getText())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/detections/{id}")
    public Mono<ResponseEntity<LanguageDetection>> getDetectionById(@PathVariable String id) {
        return languageIdentifyService.getDetectionById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/detections")
    public Mono<ResponseEntity<Flux<LanguageDetection>>> getAllDetections(
            @RequestParam(required = false) Boolean includeDeleted) {
        if (Boolean.TRUE.equals(includeDeleted)) {
            return Mono.just(ResponseEntity.ok(languageIdentifyService.getAllDetectionsIncludingDeleted()));
        }
        return Mono.just(ResponseEntity.ok(languageIdentifyService.getAllDetections()));
    }

    @GetMapping("/detections/deleted")
    public Mono<ResponseEntity<Flux<LanguageDetection>>> getDeletedDetections() {
        return Mono.just(ResponseEntity.ok(languageIdentifyService.getDeletedDetections()));
    }

    @PutMapping("/detections/{id}")
    public Mono<ResponseEntity<LanguageDetection>> updateDetection(
            @PathVariable String id,
            @RequestBody LanguageDetectionRequest request) {
        return languageIdentifyService.updateDetection(id, request.getText())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/detections/{id}/permanent")
    public Mono<ResponseEntity<Void>> permanentDeleteDetection(@PathVariable String id) {
        return languageIdentifyService.permanentDeleteDetection(id)
            .thenReturn(ResponseEntity.noContent().build());
    }

    @DeleteMapping("/detections/{id}")
    public Mono<ResponseEntity<Void>> deleteDetection(@PathVariable String id) {
        return languageIdentifyService.softDeleteDetection(id)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @PatchMapping("/detections/{id}/restore")
    public Mono<ResponseEntity<LanguageDetection>> restoreDetection(@PathVariable String id) {
        return languageIdentifyService.restoreDetection(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
