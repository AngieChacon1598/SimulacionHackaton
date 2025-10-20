package com.vallegrande.webfluxai.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "job_search_results")
public class JobSearchResult {
    @Id
    private String id;
    private String query;
    private String location;
    private Integer page;
    private Integer resultsPerPage;
    private List<Job> jobs;
    private LocalDateTime searchedAt;
    private Map<String, Object> metadata;
    private LocalDateTime updatedAt;
    private Boolean deleted;
    private LocalDateTime deletedAt;

    @Data
    public static class Job {
        private String jobId;
        private String title;
        private String companyName;
        private String location;
        private String jobType;
        private String salary;
        private String description;
        private String applyLink;
        private LocalDateTime postedAt;
        private List<String> requiredSkills;
        private Map<String, Object> additionalInfo;
    }
}
