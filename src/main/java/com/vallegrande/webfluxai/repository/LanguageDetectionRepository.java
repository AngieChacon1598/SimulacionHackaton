package com.vallegrande.webfluxai.repository;

import com.vallegrande.webfluxai.model.LanguageDetection;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface LanguageDetectionRepository extends ReactiveMongoRepository<LanguageDetection, String> {
    Flux<LanguageDetection> findByDetectedLanguageCode(String languageCode);
    Flux<LanguageDetection> findByConfidenceGreaterThan(Double minConfidence);
    Flux<LanguageDetection> findByDeletedFalse();
    Flux<LanguageDetection> findByDeletedTrue();
    Mono<LanguageDetection> findByIdAndDeletedFalse(String id);
}
