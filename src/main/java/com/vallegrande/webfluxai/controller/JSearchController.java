package com.vallegrande.webfluxai.controller;

import com.vallegrande.webfluxai.dto.JobSearchRequest;
import com.vallegrande.webfluxai.dto.JobSearchResponse;
import com.vallegrande.webfluxai.model.JobSearchResult;
import com.vallegrande.webfluxai.service.JSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JSearchController {

    private final JSearchService jSearchService;

    @GetMapping("/search")
    public Mono<ResponseEntity<JobSearchResult>> searchJobs(
            @RequestParam String query,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer resultsPerPage) {
        
        var request = new JobSearchRequest();
        request.setQuery(query);
        request.setLocation(location);
        request.setPage(page);
        request.setResultsPerPage(resultsPerPage);

        return jSearchService.searchJobs(request)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/details/{jobId}")
    public Mono<ResponseEntity<JobSearchResponse.Job>> getJobDetails(@PathVariable String jobId) {
        return jSearchService.getJobDetails(jobId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<JobSearchResult>> getSearchResultById(@PathVariable String id) {
        return jSearchService.getSearchResultById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    public Mono<ResponseEntity<Flux<JobSearchResult>>> getAllSearchResults(
            @RequestParam(required = false) Boolean includeDeleted) {
        if (Boolean.TRUE.equals(includeDeleted)) {
            return Mono.just(ResponseEntity.ok(jSearchService.getAllSearchResultsIncludingDeleted()));
        }
        return Mono.just(ResponseEntity.ok(jSearchService.getAllSearchResults()));
    }

    @GetMapping("/deleted")
    public Mono<ResponseEntity<Flux<JobSearchResult>>> getDeletedSearchResults() {
        return Mono.just(ResponseEntity.ok(jSearchService.getDeletedSearchResults()));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<JobSearchResult>> updateSearchResult(
            @PathVariable String id,
            @RequestBody JobSearchRequest request) {
        return jSearchService.updateSearchResult(id, request)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteSearchResult(@PathVariable String id) {
        return jSearchService.softDeleteSearchResult(id)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @PatchMapping("/{id}/restore")
    public Mono<ResponseEntity<JobSearchResult>> restoreSearchResult(@PathVariable String id) {
        return jSearchService.restoreSearchResult(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
