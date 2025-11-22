package com.event.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateLocationRequest {
    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    private String city;
}
