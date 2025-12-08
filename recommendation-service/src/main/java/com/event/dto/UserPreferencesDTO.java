package com.event.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesDTO {
    private UUID userId;
    private String username;
    private String email;
    private Location location;
    private List<String> interests;  // e.g., ["music", "tech", "fashion"]
    private Map<String, Object> fashionStyle;  // JSONB from database

    // Metadata
    private String city;
    private Boolean hasInteractions;  // Has the user interacted with any events?

    // Check if user is a new user
    public boolean isColdStart() {
        return hasInteractions == null || !hasInteractions;
    }

    // Check if user has location set
    public boolean hasLocation() {
        return location != null && location.isValid();
    }

    // Check if user has interests set
    public boolean hasInterests() {
        return interests != null && !interests.isEmpty();
    }
}
