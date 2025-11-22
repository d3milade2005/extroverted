package com.event.dto;

import com.event.entity.InteractionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteractionRequest {

    @NotNull(message = "Interaction type is required")
    private InteractionType type;
}