package com.event.service;

import com.event.dto.*;
import com.event.entity.ActionType;
import com.event.entity.EntityType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final EventServiceClient eventServiceClient;
    private final UserServiceClient userServiceClient;
    private final AdminActionService adminActionService;

    public List<AdminEventsResponse> getEvents(String status, int page, int size) {
        // 1. Get raw events
        List<EventDTO> events = eventServiceClient.getEvents(status, page, size);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Extract Host IDs and Fetch Users
        Set<UUID> hostIds = events.stream()
                .map(EventDTO::getHostId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, UserBatchDTO> userMap = hostIds.isEmpty()
                ? Collections.emptyMap()
                : userServiceClient.fetchUsers(hostIds);

        return events.stream()
                .map(event -> {
                    // Find the host for this specific event
                    UserBatchDTO hostUser = userMap.get(event.getHostId());

                    // Use the helper method to build the response
                    return mapToAdminResponse(event, hostUser);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void approveEvent(UUID eventId, UUID adminId, String adminName, EventActionRequest request) {
        EventDTO event = eventServiceClient.getEventById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        if ("APPROVED".equals(event.getStatus())) {
            throw new IllegalStateException("Event is already approved.");
        }

        eventServiceClient.updateEventStatus(eventId, "APPROVED");

        logEventAction(event, adminId, adminName, ActionType.APPROVE_EVENT, request);
    }


    @Transactional
    public void rejectEvent(UUID eventId, UUID adminId, String adminName, EventActionRequest request) {
        EventDTO event = eventServiceClient.getEventById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        if ("REJECTED".equals(event.getStatus())) {
            throw new IllegalStateException("Event is already rejected.");
        }

        eventServiceClient.updateEventStatus(eventId, "REJECTED");

        logEventAction(event, adminId, adminName, ActionType.REJECT_EVENT, request);
    }


    private void logEventAction(EventDTO event, UUID adminId, String adminName, ActionType actionType, EventActionRequest request) {
        Map<UUID, UserBatchDTO> users = userServiceClient.fetchUsers(Set.of(event.getHostId()));
        UserBatchDTO host = users.get(event.getHostId());

        Map<String, Object> meta = getStringObjectMap(request, host, event, adminName);

        // C. Call Your Action Service
        String description = (actionType == ActionType.APPROVE_EVENT)
                ? "Approved event: " + event.getTitle()
                : "Rejected event: " + event.getTitle();

        adminActionService.logAction(
                actionType,
                adminId,
                EntityType.EVENT,
                event.getId(),
                description,
                meta
        );
    }

    private Map<String, Object> getStringObjectMap(EventActionRequest request, UserBatchDTO host, EventDTO event, String adminName) {
        Map<String, Object> meta = new HashMap<>();

        meta.put("admin_name_snapshot", adminName);

        if (host != null) {
            meta.put("host_id", host.getHostId());
            meta.put("host_email", host.getHostEmail());
            meta.put("host_name", host.getHostName());
        } else {
            meta.put("host_info", "User not found");
        }

        // 3. Snapshot Event Context
        meta.put("event_title", event.getTitle());
        meta.put("action_reason", request.getReason()); // Important for Rejection
        meta.put("custom_message", request.getCustomMessage());

        return meta;
    }

    private AdminEventsResponse mapToAdminResponse(EventDTO event, UserBatchDTO hostUser) {
        UserBatchDTO hostDto = null;
        if (hostUser != null) {
            hostDto = new UserBatchDTO(
                    hostUser.getHostId(),
                    hostUser.getHostName(),
                    hostUser.getHostEmail()
            );
        }

        return AdminEventsResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .category(event.getCategoryName())
                .host(hostDto) // Enriched Host Data
                .venue(event.getVenue())
                .address(event.getAddress())
                .location(event.getLocation())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .createdAt(event.getCreatedAt())
                .ticketPrice(event.getTicketPrice())
                .ticketLimit(event.getTicketLimit())
                .ticketsSold(event.getTicketsSold())
                .imageUrl(event.getImageUrl())
                .verified(event.getVerified())
                .status(event.getStatus())
                .build();
    }
}