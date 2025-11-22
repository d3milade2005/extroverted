package com.event.service;

import com.event.dto.*;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuthService {
    private final Keycloak keycloak;
    private final RestTemplate restTemplate;
    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;
    @Value("${keycloak.realm}")
    private String realm;
    @Value("${keycloak.resource}")
    private String clientId;
    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    public AuthService(Keycloak keycloak) {
        this.keycloak = keycloak;
        this.restTemplate = new RestTemplate();
    }

    public TokenResponse login(LoginRequest loginRequest) {
        String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/token", authServerUrl, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "password");
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("username", loginRequest.getUsername());
        map.add("password", loginRequest.getPassword());
        map.add("scope", "openid profile email");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenEndpoint, request, Map.class);
            Map<String, Object> body = response.getBody();
            return TokenResponse.builder()
                    .accessToken((String) body.get("access_token"))
                    .refreshToken((String) body.get("refresh_token"))
                    .expiresIn(((Number) body.get("expires_in")).longValue())
                    .scope((String) body.get("scope"))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Login failed: Invalid username or password -->" + e);
        }
    }

    public String register(RegisterRequest registerRequest) {
        try {
            RealmResource realmResource = keycloak.realm(realm);

            UsersResource usersResource = realmResource.users();

            List<UserRepresentation> existingUsers = usersResource.search(registerRequest.getUsername());
            if (!existingUsers.isEmpty()) {
                throw new RuntimeException("Username already exists");
            }

            List<UserRepresentation> existingEmailUsers = usersResource.search(registerRequest.getEmail());
            if (!existingEmailUsers.isEmpty()) {
                throw new RuntimeException("Email already exists");
            }

            UserRepresentation user = new UserRepresentation();
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            user.setEmailVerified(true);
            user.setEnabled(true);
            user.setFirstName(registerRequest.getFirstName());
            user.setLastName(registerRequest.getLastName());

            Response response = usersResource.create(user);

            if (response.getStatus() == 403) {
                throw new RuntimeException("HTTP 403 Forbidden - Service account does not have permission to create users");
            }

            if (response.getStatus() != 201) {
                throw new RuntimeException("Failed to register user. Status: " + response.getStatus());
            }

            String locationHeader = response.getHeaderString("Location");
            String userId = locationHeader.substring(locationHeader.lastIndexOf("/") + 1);

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(registerRequest.getPassword());
            credential.setTemporary(false);

            usersResource.get(userId).resetPassword(credential);

            assignRoleToUser(userId, "USER");

            response.close();
            return userId;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void updatePassword(UserInfo userInfo, ChangePasswordRequest changePasswordRequest) {
        RealmResource realmResource = keycloak.realm(realm);
        UserResource userResource = realmResource.users().get(userInfo.getId());

        UserRepresentation user = userResource.toRepresentation();
        String username = user.getUsername();

        boolean oldPasswordCorrect = verifyOldPassword(username, changePasswordRequest.getOldPassword());
        if (!oldPasswordCorrect) {
            throw new RuntimeException("Old password is incorrect.");
        }

        if (changePasswordRequest.getOldPassword().equals(changePasswordRequest.getNewPassword())) {
            throw new RuntimeException("New password must be different from the old password.");
        }

        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType(credentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(changePasswordRequest.getNewPassword());
        credentialRepresentation.setTemporary(false);

        userResource.resetPassword(credentialRepresentation);
    }

    public TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/token", authServerUrl, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "refresh_token");
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("refresh_token", refreshTokenRequest.getRefreshToken());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenEndpoint, request, Map.class);
            Map<String, Object> body = response.getBody();
            return TokenResponse.builder()
                    .accessToken((String) body.get("access_token"))
                    .refreshToken((String) body.get("refresh_token"))
                    .expiresIn(((Number) body.get("expires_in")).longValue())
                    .scope((String) body.get("scope"))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Login failed: Invalid username or password -->" + e);
        }
    }

//    public void sendPasswordResetEmail(String email) {
//        try {
//            UsersResource usersResource = keycloak.realm(realm).users();
//            List<UserRepresentation> users = usersResource.search(email, true);
//
//            if (users.isEmpty()) {
//                // Don't reveal if user exists or not (security best practice)
//                log.warn("Password reset requested for non-existent email: {}", email);
//                return;
//            }
//
//            UserRepresentation user = users.get(0);
//
//            // Send password reset email
//            UserResource userResource = usersResource.get(user.getId());
//            userResource.executeActionsEmail(Collections.singletonList("UPDATE_PASSWORD"));
//
//            log.info("Password reset email sent to: {}", email);
//
//        } catch (Exception e) {
//            log.error("Error sending password reset email for: {}", email, e);
//            // Don't throw exception to avoid revealing if user exists
//        }
//    }

    public void logout(String refreshToken) {
        String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/logout", authServerUrl, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        try{
            restTemplate.postForEntity(tokenEndpoint, request, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Logout failed: Invalid username or password -->" + e);
        }
    }

    public UserInfo getCurrentUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();

            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            List<String> roles = realmAccess != null
                    ? (List<String>) realmAccess.get("roles")
                    : new ArrayList<>();

            return UserInfo.builder()
                    .id(jwt.getSubject())
                    .username(jwt.getClaimAsString("preferred_username"))
                    .email(jwt.getClaimAsString("email"))
                    .firstName(jwt.getClaimAsString("given_name"))
                    .lastName(jwt.getClaimAsString("family_name"))
                    .emailVerified(jwt.getClaimAsBoolean("email_verified"))
                    .roles(roles)
                    .build();
        }

        throw new RuntimeException("No authentication found");
    }

    public UserInfo getUserInfo(String userId) {
        RealmResource realmResource = keycloak.realm(realm);
        UserResource userResource = realmResource.users().get(userId);
        UserRepresentation user = new UserRepresentation();

        List<String> roles = userResource.roles().realmLevel().listAll()
                .stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());

        return UserInfo.builder()
                .id(userId)
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .emailVerified(user.isEmailVerified())
                .roles(roles)
                .build();
    }

    public void assignRoleToUser(String userId, String roleName) {
        RealmResource realmResource = keycloak.realm(realm);
        UserResource userResource = realmResource.users().get(userId);

        RoleRepresentation roleRepresentation = realmResource.roles().get(roleName).toRepresentation();

        userResource.roles().realmLevel().add(Collections.singletonList(roleRepresentation));
    }

    public boolean verifyOldPassword(String username, String oldPassword) {
        try {
            Keycloak keycloakVerify = KeycloakBuilder.builder()
                    .serverUrl(authServerUrl)
                    .realm(realm)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .username(username)
                    .password(oldPassword)
                    .build();
            keycloakVerify.tokenManager().getAccessToken();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
