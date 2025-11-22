package com.event.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePreferencesRequest {
    private List<String> interests;
    private Map<String, Object> fashionStyle;
}
