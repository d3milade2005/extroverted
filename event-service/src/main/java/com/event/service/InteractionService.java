package com.event.service;

import com.event.dto.InteractionRequest;
import com.event.entity.Event;
import com.event.entity.Interaction;
import com.event.entity.InteractionType;
import com.event.repository.EventRepository;
import com.event.repository.InteractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionService {

    private final InteractionRepository interactionRepository;
    private final EventRepository eventRepository;

    @Transactional
    public void recordInteraction(UUID userId, UUID eventId, InteractionRequest request) {
        log.info("Recording interaction: user={}, event={}, type={}", userId, eventId, request.getType());

        // Verify event exists
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // Check if interaction already exists
        boolean exists = interactionRepository.existsByUserIdAndEventIdAndType(
                userId, eventId, request.getType());

        if (exists) {
            log.info("Interaction already exists, skipping");
            return;
        }

        // Create interaction
        Interaction interaction = Interaction.builder()
                .userId(userId)
                .event(event)
                .type(request.getType())
                .build();

        interactionRepository.save(interaction);
        log.info("Interaction recorded successfully");
    }

    @Transactional
    public void removeInteraction(UUID userId, UUID eventId, InteractionType type) {
        log.info("Removing interaction: user={}, event={}, type={}", userId, eventId, type);

        interactionRepository.deleteByUserIdAndEventIdAndType(userId, eventId, type);
        log.info("Interaction removed successfully");
    }

    @Transactional(readOnly = true)
    public boolean hasInteraction(UUID userId, UUID eventId, InteractionType type) {
        return interactionRepository.existsByUserIdAndEventIdAndType(userId, eventId, type);
    }

    @Transactional(readOnly = true)
    public long getInteractionCount(UUID eventId, InteractionType type) {
        return interactionRepository.countByEventIdAndType(eventId, type);
    }
}