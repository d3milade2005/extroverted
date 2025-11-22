package com.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String keycloakId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String city;
    private LocationDTO location;
    private Map<String, Object> fashionStyle;
    private List<String> interests;
    private String role;
    private Boolean verified;
}