package com.vallegrande.webfluxai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LanguageDetectionRequest {
    @NotBlank(message = "Text cannot be empty")
    private String text;
}
