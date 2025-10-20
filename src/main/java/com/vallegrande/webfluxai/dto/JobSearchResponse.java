package com.vallegrande.webfluxai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobSearchResponse {
    private String status;
    @JsonProperty("request_id")
    private String requestId;
    private Parameters parameters;
    private List<Job> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Parameters {
        private String query;
        private int page;
        @JsonProperty("num_pages")
        private int numPages;
        @JsonProperty("date_posted")
        private String datePosted;
        private String country;
        private String language;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Job {
        @JsonProperty("job_id")
        private String jobId;
        
        @JsonProperty("job_title")
        private String jobTitle;
        
        @JsonProperty("employer_name")
        private String employerName;
        
        @JsonProperty("employer_logo")
        private String employerLogo;
        
        @JsonProperty("employer_website")
        private String employerWebsite;
        
        @JsonProperty("job_publisher")
        private String jobPublisher;
        
        @JsonProperty("job_employment_type")
        private String jobEmploymentType;
        
        @JsonProperty("job_employment_types")
        private List<String> jobEmploymentTypes;
        
        @JsonProperty("job_apply_link")
        private String jobApplyLink;
        
        @JsonProperty("job_apply_is_direct")
        private boolean jobApplyIsDirect;
        
        @JsonProperty("apply_options")
        private List<ApplyOption> applyOptions;
        
        @JsonProperty("job_description")
        private String jobDescription;
        
        @JsonProperty("job_is_remote")
        private boolean jobIsRemote;
        
        @JsonProperty("job_posted_at")
        private String jobPostedAt;
        
        @JsonProperty("job_posted_at_timestamp")
        private long jobPostedAtTimestamp;
        
        @JsonProperty("job_posted_at_datetime_utc")
        private String jobPostedAtDatetimeUtc;
        
        @JsonProperty("job_location")
        private String jobLocation;
        
        @JsonProperty("job_city")
        private String jobCity;
        
        @JsonProperty("job_state")
        private String jobState;
        
        @JsonProperty("job_country")
        private String jobCountry;
        
        @JsonProperty("job_latitude")
        private double jobLatitude;
        
        @JsonProperty("job_longitude")
        private double jobLongitude;
        
        @JsonProperty("job_benefits")
        private List<String> jobBenefits;
        
        @JsonProperty("job_google_link")
        private String jobGoogleLink;
        
        @JsonProperty("job_salary")
        private String jobSalary;
        
        @JsonProperty("job_min_salary")
        private Integer jobMinSalary;
        
        @JsonProperty("job_max_salary")
        private Integer jobMaxSalary;
        
        @JsonProperty("job_salary_period")
        private String jobSalaryPeriod;
        
        @JsonProperty("job_highlights")
        private JobHighlights jobHighlights;
        
        @JsonProperty("job_onet_soc")
        private String jobOnetSoc;
        
        @JsonProperty("job_onet_job_zone")
        private String jobOnetJobZone;
        
        // Métodos para compatibilidad con nuestro modelo interno
        public String getTitle() {
            return jobTitle;
        }
        
        public String getLocation() {
            return jobLocation;
        }
        
        public String getJobType() {
            return jobEmploymentType;
        }
        
        public String getJobDescription() {
            return jobDescription;
        }
        
        public String getJobApplyLink() {
            return jobApplyLink;
        }
        
        public String getJobPostedAt() {
            return jobPostedAtDatetimeUtc;
        }
        
        public List<String> getJobRequiredSkills() {
            if (jobHighlights != null && jobHighlights.getQualifications() != null) {
                return jobHighlights.getQualifications();
            }
            return null;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApplyOption {
        private String publisher;
        @JsonProperty("apply_link")
        private String applyLink;
        @JsonProperty("is_direct")
        private boolean isDirect;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JobHighlights {
        private List<String> Qualifications;
        private List<String> Benefits;
        private List<String> Responsibilities;
    }
}
