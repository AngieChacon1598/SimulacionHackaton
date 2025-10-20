package com.vallegrande.webfluxai.repository;

import com.vallegrande.webfluxai.model.JobSearchResult;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.time.LocalDateTime;
import reactor.core.publisher.Mono;

@Repository
public interface JobSearchResultRepository extends ReactiveMongoRepository<JobSearchResult, String> {
    Flux<JobSearchResult> findByQueryContainingIgnoreCase(String query);
    Flux<JobSearchResult> findByLocationContainingIgnoreCase(String location);
    Flux<JobSearchResult> findBySearchedAtAfter(LocalDateTime date);
    Flux<JobSearchResult> findByDeletedFalse();
    Flux<JobSearchResult> findByDeletedTrue();
    Mono<JobSearchResult> findByIdAndDeletedFalse(String id);
}
