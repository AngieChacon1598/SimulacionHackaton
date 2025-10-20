package com.vallegrande.webfluxai.dto;

import lombok.Data;

@Data
public class JobSearchRequest {
    private String query;
    private String location;
    private Integer page = 1;
    private Integer resultsPerPage = 10;
}
