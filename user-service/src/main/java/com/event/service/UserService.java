package com.event.service;

import com.event.dto.*;
import com.event.entity.User;
import com.event.entity.UserRole;
import com.event.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Transactional
    public UserProfileResponse initializeUser(String keycloakId, String username, String email, String firstName, String lastName, InitializeUserRequest initializeUserRequest) {
        if (userRepository.existsByKeycloakId(keycloakId)) {
            throw new RuntimeException("User already exists");
        }

        Point location = null;
        if (initializeUserRequest.getLatitude() != null && initializeUserRequest.getLongitude() != null) {
            location = createPoint(initializeUserRequest.getLatitude(), initializeUserRequest.getLongitude());
        }

        User user = User.builder()
                .keycloakId(keycloakId)
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .city(initializeUserRequest.getCity())
                .location(location)
                .interests(initializeUserRequest.getInterests())
                .fashionStyle(initializeUserRequest.getFashionStyle())
                .role(UserRole.USER)
                .verified(false)
                .build();

        user = userRepository.save(user);
        log.info("User inserted successfully");
        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String keycloakId) {
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() ->  new RuntimeException("User not found"));
        return mapToResponse(user);
    }

    @Transactional
    public UserProfileResponse updatePreferences(String keycloakId, @Valid UpdatePreferencesRequest request) {
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getInterests() != null) {
            user.setInterests(request.getInterests());
        }

        if (request.getFashionStyle() != null) {
            user.setFashionStyle(request.getFashionStyle());
        }

        user = userRepository.save(user);
        log.info("Updated preferences for user: {}", keycloakId);

        return mapToResponse(user);
    }

    @Transactional
    public UserProfileResponse updateLocation(String keycloakId, UpdateLocationRequest request) {
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Point location = createPoint(request.getLatitude(), request.getLongitude());
        user.setLocation(location);

        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }

        user = userRepository.save(user);
        log.info("Updated location for user: {}", keycloakId);

        return mapToResponse(user);
    }

    @Transactional
    public void deleteUser(String keycloakId) {
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
        log.info("Deleted user: {}", keycloakId);
    }

    private Point createPoint(Double latitude, Double longitude) {
        return geometryFactory.createPoint(new Coordinate(longitude, latitude));
    }


    private UserProfileResponse mapToResponse(User user) {
        LocationDTO locationDTO = null;
        if (user.getLocation() != null) {
            locationDTO = new LocationDTO(
                    user.getLocation().getY(), // latitude
                    user.getLocation().getX()  // longitude
            );
        }
        return UserProfileResponse.builder()
                .keycloakId(user.getKeycloakId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .city(user.getCity())
                .location(locationDTO)
                .fashionStyle(user.getFashionStyle())
                .interests(user.getInterests())
                .role(user.getRole().name())
                .verified(user.getVerified())
                .build();
    }
}
