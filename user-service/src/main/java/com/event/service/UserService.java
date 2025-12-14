package com.event.service;

import com.event.dto.*;
import com.event.entity.User;
import com.event.entity.UserRole;
import com.event.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Transactional
    public UserProfileResponse initializeUser(String keycloakId, String username, String email, String firstName, String lastName, InitializeUserRequest initializeUserRequest) {
        UUID id = UUID.fromString(keycloakId);

        if (userRepository.existsById(id)) {
            throw new RuntimeException("User already exists");
        }

        Point location = null;
        if (initializeUserRequest.getLatitude() != null && initializeUserRequest.getLongitude() != null) {
            location = createPoint(initializeUserRequest.getLatitude(), initializeUserRequest.getLongitude());
        }

        User user = User.builder()
                .id(id)
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
                .active(true)
                .build();

        user = userRepository.save(user);
        log.info("User inserted successfully");
        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String keycloakId) {
        UUID id = UUID.fromString(keycloakId);
        User user = userRepository.findById(id)
                .orElseThrow(() ->  new RuntimeException("User not found"));
        return mapToResponse(user);
    }

    @Transactional
    public UserProfileResponse updatePreferences(String keycloakId, @Valid UpdatePreferencesRequest request) {
        UUID id = UUID.fromString(keycloakId);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getInterests() != null) {
            user.setInterests(request.getInterests());
        }

        if (request.getFashionStyle() != null) {
            user.setFashionStyle(request.getFashionStyle());
        }

        user = userRepository.save(user);

        return mapToResponse(user);
    }

    @Transactional
    public UserProfileResponse updateLocation(String keycloakId, UpdateLocationRequest request) {
        UUID id = UUID.fromString(keycloakId);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Point location = createPoint(request.getLatitude(), request.getLongitude());
        user.setLocation(location);

        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }

        user = userRepository.save(user);

        return mapToResponse(user);
    }

    @Transactional
    public void deleteUser(String keycloakId) {
        UUID id = UUID.fromString(keycloakId);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
    }

    private Point createPoint(Double latitude, Double longitude) {
        return geometryFactory.createPoint(new Coordinate(longitude, latitude));
    }

    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getAllUsers(String query, Boolean verified, Boolean active, Pageable pageable) {
        if (query != null && !query.isBlank()) {
            return userRepository.searchUsers(query, pageable).map(this::mapToResponse);
        }
        if (verified != null) {
            return userRepository.findByVerified(verified, pageable).map(this::mapToResponse);
        }
        if (active != null) {
            return userRepository.findByActive(active, pageable).map(this::mapToResponse);
        }
        return userRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional
    public void banUser(UUID userId, BanReason reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!user.getActive()) {
            throw new IllegalStateException("User is already banned");
        }

        user.setActive(false);
        // send email to user on why they were banned
        userRepository.save(user);
    }

    @Transactional
    public void unbanUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        user.setActive(true);
        userRepository.save(user);
    }

    @Transactional
    public void toggleVerification(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        user.setVerified(!user.getVerified());
        userRepository.save(user);
    }

    @Transactional
    public UserProfileResponse getUser (UUID userId) {
        return mapToResponse(userRepository.findById(userId).orElseThrow( () -> new EntityNotFoundException("User not found")));
    }

    @Transactional
    public Boolean isActiveAdmin (UUID userId) {
        return userRepository.isAdminAndActive(userId);
    }

    @Transactional
    public Page<UserProfileResponse> getAllAdmins(Pageable pageable) {
        return userRepository.findAllByRole(UserRole.ADMIN, pageable).map(this::mapToResponse);
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
                .id(user.getId())
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
                .active(user.getActive())
                .build();
    }
}
