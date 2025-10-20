package com.vallegrande.webfluxai.service;

import com.vallegrande.webfluxai.config.ApiProperties;
import com.vallegrande.webfluxai.dto.JobSearchRequest;
import com.vallegrande.webfluxai.dto.JobSearchResponse;
import com.vallegrande.webfluxai.exception.ApiException;
import com.vallegrande.webfluxai.model.JobSearchResult;
import com.vallegrande.webfluxai.repository.JobSearchResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JSearchService {

    @Qualifier("jsearchWebClient")
    private final WebClient jsearchWebClient;
    private final JobSearchResultRepository repository;
    private final ApiProperties apiProperties;

    public Mono<JobSearchResult> searchJobs(JobSearchRequest request) {
        log.info("Searching jobs with query: {}, location: {}", request.getQuery(), request.getLocation());

        final String country = determineCountry(request.getLocation());
        log.info("Using country code: {} for location: {}", country, request.getLocation());

        int numPages = Math.min(request.getResultsPerPage(), 5);

        return jsearchWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("query", request.getQuery())
                        .queryParam("page", request.getPage())
                        .queryParam("num_pages", numPages)
                        .queryParam("country", country)
                        .queryParam("date_posted", "all")
                        .build())
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(error -> {
                                    log.error("Error searching jobs: {}", error);
                                    return Mono.error(new ApiException(
                                            HttpStatus.valueOf(clientResponse.statusCode().value()),
                                            "Error searching jobs: " + error
                                    ));
                                })
                )
                .bodyToMono(String.class)
                .doOnNext(responseBody -> log.info("Raw response received, length: {} bytes", responseBody.length()))
                .flatMap(responseBody -> {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        JobSearchResponse response = mapper.readValue(responseBody, JobSearchResponse.class);

                        log.info("JSON parsed successfully. Status: {}, Data size: {}",
                                response.getStatus(), response.getData() != null ? response.getData().size() : 0);

                        if (response.getData() == null || response.getData().isEmpty()) {
                            log.info("No jobs found for query: '{}' in location: '{}' (country: {})", 
                                    request.getQuery(), request.getLocation(), country);
                            
                            // Crear un resultado vacío en lugar de lanzar error
                            var emptyResult = new JobSearchResult();
                            emptyResult.setQuery(request.getQuery());
                            emptyResult.setLocation(request.getLocation());
                            emptyResult.setPage(request.getPage());
                            emptyResult.setResultsPerPage(numPages);
                            emptyResult.setSearchedAt(LocalDateTime.now());
                            emptyResult.setUpdatedAt(LocalDateTime.now());
                            emptyResult.setDeleted(false);
                            emptyResult.setJobs(java.util.Collections.emptyList());
                            
                            Map<String, Object> metadata = new HashMap<>();
                            metadata.put("status", response.getStatus());
                            metadata.put("requestId", response.getRequestId());
                            metadata.put("searchQuery", request.getQuery());
                            metadata.put("searchPage", request.getPage());
                            metadata.put("searchCountry", country);
                            metadata.put("totalResults", 0);
                            metadata.put("message", "No jobs found matching the criteria");
                            emptyResult.setMetadata(metadata);
                            
                            return repository.save(emptyResult);
                        }

                        var result = new JobSearchResult();
                        result.setQuery(request.getQuery());
                        result.setLocation(request.getLocation());
                        result.setPage(request.getPage());
                        result.setResultsPerPage(numPages);
                        result.setSearchedAt(LocalDateTime.now());
                        result.setUpdatedAt(LocalDateTime.now());
                        result.setDeleted(false);

                        var jobs = response.getData().stream()
                                .map(job -> {
                                    var jobResult = new JobSearchResult.Job();
                                    jobResult.setJobId(job.getJobId());
                                    jobResult.setTitle(job.getTitle());
                                    jobResult.setCompanyName(job.getEmployerName());
                                    jobResult.setLocation(job.getLocation());
                                    jobResult.setJobType(job.getJobType());
                                    jobResult.setDescription(job.getJobDescription());
                                    jobResult.setApplyLink(job.getJobApplyLink());
                                    jobResult.setPostedAt(parseDateTime(job.getJobPostedAt()));
                                    jobResult.setRequiredSkills(job.getJobRequiredSkills());
                                    return jobResult;
                                })
                                .collect(Collectors.toList());

                        result.setJobs(jobs);

                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("status", response.getStatus());
                        metadata.put("requestId", response.getRequestId());
                        if (response.getParameters() != null) {
                            metadata.put("searchQuery", response.getParameters().getQuery());
                            metadata.put("searchPage", response.getParameters().getPage());
                            metadata.put("searchCountry", response.getParameters().getCountry());
                        }
                        metadata.put("totalResults", response.getData().size());
                        metadata.put("responseSizeBytes", responseBody.length());
                        result.setMetadata(metadata);

                        return repository.save(result);
                    } catch (Exception e) {
                        log.error("Error processing response: {}", e.getMessage(), e);
                        return Mono.error(new ApiException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Error processing response: " + e.getMessage()
                        ));
                    }
                })
                .onErrorMap(throwable -> {
                    log.error("Unexpected error in searchJobs: {}", throwable.getMessage(), throwable);
                    if (throwable instanceof ApiException) {
                        return throwable;
                    }
                    return new ApiException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "An unexpected error occurred: " + throwable.getMessage()
                    );
                });
    }

    public Mono<JobSearchResponse.Job> getJobDetails(String jobId) {
        log.info("Getting job details for jobId: {}", jobId);

        return jsearchWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/job-details")
                        .queryParam("job_id", jobId)
                        .queryParam("country", "us")
                        .build())
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(error -> {
                                    log.error("Error getting job details: {}", error);
                                    return Mono.error(new ApiException(
                                            HttpStatus.valueOf(clientResponse.statusCode().value()),
                                            "Error getting job details: " + error
                                    ));
                                })
                )
                .bodyToMono(JobSearchResponse.class)
                .flatMap(response -> {
                    if (response.getData() == null || response.getData().isEmpty()) {
                        return Mono.error(new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Job details not found for jobId: " + jobId
                        ));
                    }
                    return Mono.just(response.getData().get(0));
                });
    }

    private String determineCountry(String location) {
        if (location == null) return "us";

        String loc = location.toLowerCase();
        
        // América del Sur
        if (loc.contains("argentina") || loc.contains("buenos aires") || loc.contains("cordoba") || loc.contains("rosario")) {
            return "ar";
        } else if (loc.contains("bolivia") || loc.contains("la paz") || loc.contains("santa cruz") || loc.contains("cochabamba")) {
            return "bo";
        } else if (loc.contains("brasil") || loc.contains("brazil") || loc.contains("sao paulo") || loc.contains("rio de janeiro")) {
            return "br";
        } else if (loc.contains("chile") || loc.contains("santiago") || loc.contains("valparaiso") || loc.contains("concepcion")) {
            return "cl";
        } else if (loc.contains("colombia") || loc.contains("bogota") || loc.contains("medellin") || loc.contains("cali")) {
            return "co";
        } else if (loc.contains("ecuador") || loc.contains("quito") || loc.contains("guayaquil") || loc.contains("cuenca")) {
            return "ec";
        } else if (loc.contains("paraguay") || loc.contains("asuncion") || loc.contains("ciudad del este")) {
            return "py";
        } else if (loc.contains("peru") || loc.contains("lima") || loc.contains("arequipa") || loc.contains("trujillo")) {
            return "pe";
        } else if (loc.contains("uruguay") || loc.contains("montevideo") || loc.contains("salto")) {
            return "uy";
        } else if (loc.contains("venezuela") || loc.contains("caracas") || loc.contains("maracaibo")) {
            return "ve";
        }
        
        // América Central y el Caribe
        else if (loc.contains("costa rica") || loc.contains("san jose")) {
            return "cr";
        } else if (loc.contains("cuba") || loc.contains("habana") || loc.contains("havana")) {
            return "cu";
        } else if (loc.contains("el salvador") || loc.contains("san salvador")) {
            return "sv";
        } else if (loc.contains("guatemala") || loc.contains("ciudad de guatemala")) {
            return "gt";
        } else if (loc.contains("honduras") || loc.contains("tegucigalpa")) {
            return "hn";
        } else if (loc.contains("mexico") || loc.contains("cdmx") || loc.contains("guadalajara") || loc.contains("monterrey")) {
            return "mx";
        } else if (loc.contains("nicaragua") || loc.contains("managua")) {
            return "ni";
        } else if (loc.contains("panama") || loc.contains("ciudad de panama")) {
            return "pa";
        }
        
        // Europa
        else if (loc.contains("españa") || loc.contains("spain") || loc.contains("madrid") || loc.contains("barcelona")) {
            return "es";
        } else if (loc.contains("francia") || loc.contains("france") || loc.contains("paris")) {
            return "fr";
        } else if (loc.contains("alemania") || loc.contains("germany") || loc.contains("berlin")) {
            return "de";
        } else if (loc.contains("italia") || loc.contains("italy") || loc.contains("roma") || loc.contains("milan")) {
            return "it";
        } else if (loc.contains("reino unido") || loc.contains("uk") || loc.contains("london") || loc.contains("londres")) {
            return "gb";
        } else if (loc.contains("portugal") || loc.contains("lisboa")) {
            return "pt";
        }
        
        // Por defecto, si no se encuentra ninguna coincidencia
        return "us";
    }

    // ✅ Método parseDateTime actualizado para manejar fechas ISO 8601
    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            if (dateTimeStr == null) return null;
            return ZonedDateTime.parse(dateTimeStr).toLocalDateTime();
        } catch (Exception e) {
            log.warn("Error parsing date time: {}", dateTimeStr, e);
            return null;
        }
    }

    public Mono<JobSearchResult> getSearchResultById(String id) {
        return repository.findByIdAndDeletedFalse(id)
                .switchIfEmpty(Mono.error(new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Job search result not found with id: " + id
                )));
    }

    public Flux<JobSearchResult> getAllSearchResults() {
        return repository.findByDeletedFalse();
    }

    public Flux<JobSearchResult> getDeletedSearchResults() {
        return repository.findByDeletedTrue();
    }

    public Flux<JobSearchResult> getAllSearchResultsIncludingDeleted() {
        return repository.findAll();
    }

    // Método updateSearchResult actualizado para ejecutar nueva búsqueda cuando cambien parámetros
    public Mono<JobSearchResult> updateSearchResult(String id, JobSearchRequest request) {
        log.info("Updating job search result id: {} with new request", id);
        return getSearchResultById(id)
                .flatMap(existing -> {
                    // Verificar si los parámetros de búsqueda han cambiado
                    boolean searchParamsChanged = !request.getQuery().equals(existing.getQuery()) ||
                            !java.util.Objects.equals(request.getLocation(), existing.getLocation()) ||
                            !request.getPage().equals(existing.getPage()) ||
                            !request.getResultsPerPage().equals(existing.getResultsPerPage());
                    
                    if (searchParamsChanged) {
                        log.info("Search parameters changed, executing new search for id: {}", id);
                        // Ejecutar nueva búsqueda con los parámetros actualizados
                        return searchJobs(request)
                                .flatMap(newResult -> {
                                    // Mantener el ID original y fechas de creación
                                    newResult.setId(id);
                                    newResult.setSearchedAt(existing.getSearchedAt());
                                    newResult.setUpdatedAt(LocalDateTime.now());
                                    return repository.save(newResult);
                                });
                    } else {
                        log.info("No search parameters changed, updating metadata only for id: {}", id);
                        // Solo actualizar metadatos si no hay cambios en parámetros de búsqueda
                        existing.setQuery(request.getQuery());
                        existing.setLocation(request.getLocation());
                        existing.setPage(request.getPage());
                        existing.setResultsPerPage(request.getResultsPerPage());
                        existing.setUpdatedAt(LocalDateTime.now());
                        return repository.save(existing);
                    }
                });
    }

    public Mono<Void> softDeleteSearchResult(String id) {
        log.info("Soft-deleting job search result id: {}", id);
        return getSearchResultById(id)
                .flatMap(existing -> {
                    existing.setDeleted(true);
                    existing.setDeletedAt(LocalDateTime.now());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return repository.save(existing);
                })
                .then();
    }

    public Mono<JobSearchResult> restoreSearchResult(String id) {
        log.info("Restoring job search result id: {}", id);
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Job search result not found with id: " + id
                )))
                .flatMap(existing -> {
                    if (!Boolean.TRUE.equals(existing.getDeleted())) {
                        return Mono.error(new ApiException(
                                HttpStatus.BAD_REQUEST,
                                "Job search result is not deleted and cannot be restored"
                        ));
                    }
                    existing.setDeleted(false);
                    existing.setDeletedAt(null);
                    existing.setUpdatedAt(LocalDateTime.now());
                    return repository.save(existing);
                });
    }
}
