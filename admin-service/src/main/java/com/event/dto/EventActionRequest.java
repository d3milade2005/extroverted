package com.event.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventActionRequest {
    @NotBlank(message = "Reason is required")
    private String reason;
    private String customMessage;
}
