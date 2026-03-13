package com.loopy.config;

// Dependencies: @NotNull, @Size — see DEPENDENCY_GUIDE.md
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request body for reorder endpoints.
 * Contains an ordered list of IDs representing the new display order.
 */
public record ReorderRequest(
        @NotNull @Size(min = 1) List<UUID> orderedIds
) {}
