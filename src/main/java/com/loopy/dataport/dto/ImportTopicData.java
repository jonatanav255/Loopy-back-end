package com.loopy.dataport.dto;

// Dependencies: @NotBlank, @Size, @Valid — see DEPENDENCY_GUIDE.md
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ImportTopicData(
        @NotBlank(message = "Topic name is required")
        @Size(max = 100)
        String name,

        @Size(max = 500)
        String description,

        @Size(max = 7)
        String colorHex,

        List<@Valid ImportConceptData> concepts
) {}
