package com.event.dto;

import com.event.admin.model.ResolutionAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResolveReportRequest {

    @NotNull(message = "Resolution action is required")
    private ResolutionAction resolutionAction;

    @NotNull(message = "Resolution notes are required")
    @Size(min = 10, max = 1000, message = "Notes must be between 10 and 1000 characters")
    private String resolutionNotes;
}
