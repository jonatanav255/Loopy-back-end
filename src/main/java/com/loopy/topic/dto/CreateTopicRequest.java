package com.loopy.topic.dto;

// Dependencies: @NotBlank, @Size — see DEPENDENCY_GUIDE.md
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTopicRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @Size(max = 7) String colorHex
) {}
