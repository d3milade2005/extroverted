package com.event.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InitializeUserRequest {
    private String city;

    private Double latitude;
    private Double longitude;

    private List<String> interests; // ["music", "fashion", "tech"]

    private Map<String, Object> fashionStyle;
}
