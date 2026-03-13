package com.loopy.dataport.dto;

// Dependencies: @NotNull, @NotEmpty, @Valid — see DEPENDENCY_GUIDE.md
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Top-level import payload — matches the export format.
 */
public record ImportRequest(
        @NotNull(message = "exportVersion is required")
        String exportVersion,

        @NotEmpty(message = "topics list must not be empty")
        List<@Valid ImportTopicData> topics
) {}
