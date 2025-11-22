package com.event.repository;

import com.event.entity.Interaction;
import com.event.entity.InteractionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InteractionRepository extends JpaRepository<Interaction, UUID> {

    // Find all interactions by user
    List<Interaction> findByUserId(UUID userId);

    // Find specific interaction type by user and event
    Optional<Interaction> findByUserIdAndEventIdAndType(UUID userId, UUID eventId, InteractionType type);

    // Find all interactions for an event
    List<Interaction> findByEventId(UUID eventId);

    // Find interactions by user and type
    List<Interaction> findByUserIdAndType(UUID userId, InteractionType type);

    // Check if user has interacted with event
    boolean existsByUserIdAndEventId(UUID userId, UUID eventId);

    // Check if user has specific interaction type with event
    boolean existsByUserIdAndEventIdAndType(UUID userId, UUID eventId, InteractionType type);

    // Count interactions by event
    long countByEventId(UUID eventId);

    // Count interactions by event and type
    long countByEventIdAndType(UUID eventId, InteractionType type);

    // Get saved events for user (for recommendation service)
    @Query("SELECT i.event FROM Interaction i WHERE i.userId = :userId AND i.type = 'SAVE' ORDER BY i.createdAt DESC")
    List<Object> findSavedEventsByUserId(@Param("userId") UUID userId);

    // Get user's event interaction history (for analytics)
    @Query("SELECT i FROM Interaction i WHERE i.userId = :userId ORDER BY i.createdAt DESC")
    List<Interaction> findUserInteractionHistory(@Param("userId") UUID userId);

    // Delete specific interaction
    void deleteByUserIdAndEventIdAndType(UUID userId, UUID eventId, InteractionType type);
}