package com.vallegrande.webfluxai.controller;

import com.vallegrande.webfluxai.config.ApiProperties;
import com.vallegrande.webfluxai.dto.JobSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestController {

    @Qualifier("jsearchWebClient")
    private final WebClient jsearchWebClient;
    private final ApiProperties apiProperties;

    @GetMapping("/jsearch-config")
    public Mono<ResponseEntity<String>> testJSearchConfig() {
        log.info("Testing JSearch configuration");
        
        return Mono.just(ResponseEntity.ok(
                String.format("JSearch Config - Base URL: %s, API Key: %s, API Host: %s",
                        apiProperties.getJsearch().getBaseUrl(),
                        apiProperties.getJsearch().getApiKey().substring(0, 10) + "...",
                        apiProperties.getJsearch().getApiHost())
        ));
    }

    @GetMapping("/jsearch-connection")
    public Mono<ResponseEntity<String>> testJSearchConnection() {
        log.info("Testing JSearch API connection");
        
        return jsearchWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("query", "test")
                        .queryParam("page", 1)
                        .queryParam("num_pages", 1)
                        .queryParam("country", "us")
                        .queryParam("date_posted", "all")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    log.info("JSearch API response received: {}", response.substring(0, Math.min(200, response.length())));
                    return ResponseEntity.ok("JSearch API connection successful. Response length: " + response.length());
                })
                .onErrorResume(error -> {
                    log.error("JSearch API connection failed: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(500)
                            .body("JSearch API connection failed: " + error.getMessage()));
                });
    }

    @GetMapping("/jsearch-raw-response")
    public Mono<ResponseEntity<String>> getJSearchRawResponse() {
        log.info("Getting raw JSearch API response");
        
        return jsearchWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("query", "developer jobs in chicago")
                        .queryParam("page", 1)
                        .queryParam("num_pages", 1)
                        .queryParam("country", "us")
                        .queryParam("date_posted", "all")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    log.info("Raw JSearch API response: {}", response);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    log.error("JSearch API raw response failed: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(500)
                            .body("JSearch API raw response failed: " + error.getMessage()));
                });
    }

    @GetMapping("/jsearch-test-parsing")
    public Mono<ResponseEntity<String>> testJSearchParsing() {
        log.info("Testing JSearch JSON parsing step by step");
        
        return jsearchWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("query", "developer jobs in chicago")
                        .queryParam("page", 1)
                        .queryParam("num_pages", 1)
                        .queryParam("country", "us")
                        .queryParam("date_posted", "all")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(responseBody -> {
                    try {
                        log.info("Step 1: Raw response received, length: {}", responseBody.length());
                        log.info("Step 2: First 500 chars: {}", responseBody.substring(0, Math.min(500, responseBody.length())));
                        
                        // Intentar parsear con ObjectMapper
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        JobSearchResponse response = mapper.readValue(responseBody, JobSearchResponse.class);
                        
                        log.info("Step 3: JSON parsed successfully");
                        log.info("Step 4: Status: {}", response.getStatus());
                        log.info("Step 5: Data size: {}", response.getData() != null ? response.getData().size() : 0);
                        
                        if (response.getData() != null && !response.getData().isEmpty()) {
                            log.info("Step 6: First job title: {}", response.getData().get(0).getTitle());
                        }
                        
                        return Mono.just(ResponseEntity.ok("JSON parsing successful! Status: " + response.getStatus() + 
                                ", Jobs found: " + (response.getData() != null ? response.getData().size() : 0)));
                        
                    } catch (Exception e) {
                        log.error("JSON parsing failed: {}", e.getMessage(), e);
                        return Mono.just(ResponseEntity.status(500)
                                .body("JSON parsing failed: " + e.getMessage() + "\n\nFirst 1000 chars of response:\n" + 
                                        responseBody.substring(0, Math.min(1000, responseBody.length()))));
                    }
                })
                .onErrorResume(error -> {
                    log.error("Request failed: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(500)
                            .body("Request failed: " + error.getMessage()));
                });
    }

    @GetMapping("/mongodb-info")
    public Mono<ResponseEntity<String>> getMongoDBInfo() {
        log.info("Getting MongoDB information");
        
        // Aquí podrías agregar lógica para obtener información de la base de datos
        // Por ahora retornamos información básica
        return Mono.just(ResponseEntity.ok(
                "MongoDB Connection Info:\n" +
                "- Database: webflux-ai\n" +
                "- Collections: job_search_results, language_detections\n" +
                "- Connection: MongoDB Atlas (Cloud)\n\n" +
                "Para ver los datos guardados, usa:\n" +
                "- GET /api/v1/jobs/all (todas las búsquedas de trabajos)\n" +
                "- GET /api/v1/jobs/{id} (búsqueda específica de trabajos)\n" +
                "- GET /api/v1/language/detections (todas las detecciones de idioma)\n" +
                "- GET /api/v1/language/detections/{id} (detección específica de idioma)\n\n" +
                "Para probar Language Identify:\n" +
                "- POST /api/v1/language/detect con body: {\"text\": \"Hello world\"}"
        ));
    }

    @GetMapping("/language-test")
    public Mono<ResponseEntity<String>> testLanguageDetection() {
        log.info("Testing Language Identify API");
        
        return Mono.just(ResponseEntity.ok(
                "Para probar Language Identify, usa:\n\n" +
                "POST http://localhost:8088/api/v1/language/detect\n" +
                "Content-Type: application/json\n\n" +
                "Body:\n" +
                "{\n" +
                "  \"text\": \"Hello, how are you today?\"\n" +
                "}\n\n" +
                "O para español:\n" +
                "{\n" +
                "  \"text\": \"Hola, ¿cómo estás?\"\n" +
                "}\n\n" +
                "Después verifica en:\n" +
                "GET http://localhost:8088/api/v1/language/detections"
        ));
    }

    @GetMapping("/jsearch-test-countries")
    public Mono<ResponseEntity<String>> testJSearchCountries() {
        log.info("Testing JSearch with different countries");
        
        return Mono.just(ResponseEntity.ok(
                "Prueba JSearch con diferentes países:\n\n" +
                "1. **Perú (Trujillo)**:\n" +
                "   GET http://localhost:8088/api/v1/jobs/search?query=developer&location=trujillo\n\n" +
                "2. **Estados Unidos (Chicago)**:\n" +
                "   GET http://localhost:8088/api/v1/jobs/search?query=developer&location=chicago\n\n" +
                "3. **México (CDMX)**:\n" +
                "   GET http://localhost:8088/api/v1/jobs/search?query=developer&location=mexico\n\n" +
                "4. **España (Madrid)**:\n" +
                "   GET http://localhost:8088/api/v1/jobs/search?query=developer&location=madrid\n\n" +
                "5. **Sin ubicación (US por defecto)**:\n" +
                "   GET http://localhost:8088/api/v1/jobs/search?query=developer\n\n" +
                "El servicio detecta automáticamente el país basado en la ubicación."
        ));
    }
}
