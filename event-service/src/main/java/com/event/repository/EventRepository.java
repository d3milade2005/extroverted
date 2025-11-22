package com.event.repository;

import com.event.entity.Event;
import com.event.entity.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    // Find events by host
    Page<Event> findByHostId(UUID hostId, Pageable pageable);

    // Find events by category
    Page<Event> findByCategoryId(UUID categoryId, Pageable pageable);

    // Find events by status
    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    // Find approved events
    Page<Event> findByStatusAndVerified(EventStatus status, Boolean verified, Pageable pageable);

    // Find upcoming events
    @Query("SELECT e FROM Event e WHERE e.startTime > :now AND e.status = :status ORDER BY e.startTime ASC")
    Page<Event> findUpcomingEvents(@Param("now") LocalDateTime now, @Param("status") EventStatus status, Pageable pageable);

    // Find events within radius using PostGIS (distance in meters)
    @Query(value = """
        SELECT e.*, ST_Distance(e.location::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) / 1000.0 AS distance_km
        FROM events e
        WHERE ST_DWithin(
            e.location::geography,
            ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
            :radiusMeters
        )
        AND e.status = 'APPROVED'
        AND e.start_time > :now
        ORDER BY distance_km ASC
        """, nativeQuery = true)
    List<Object[]> findEventsNearby(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("radiusMeters") Double radiusMeters,
            @Param("now") LocalDateTime now
    );

    // Search events with filters
    @Query(value = """
        SELECT e.*, 
        CASE 
            WHEN :latitude IS NOT NULL AND :longitude IS NOT NULL 
            THEN ST_Distance(e.location::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) / 1000.0 
            ELSE NULL 
        END AS distance_km
        FROM events e
        WHERE 
            (:categoryId IS NULL OR e.category_id = CAST(:categoryId AS uuid))
            AND (:status IS NULL OR e.status = :status)
            AND (:verifiedOnly IS NULL OR :verifiedOnly = false OR e.verified = true)
            AND (:freeOnly IS NULL OR :freeOnly = false OR e.ticket_price = 0)
            AND (:availableTicketsOnly IS NULL OR :availableTicketsOnly = false OR e.ticket_limit IS NULL OR e.tickets_sold < e.ticket_limit)
            AND (:startDate IS NULL OR e.start_time >= :startDate)
            AND (:endDate IS NULL OR e.end_time <= :endDate)
            AND (:minPrice IS NULL OR e.ticket_price >= :minPrice)
            AND (:maxPrice IS NULL OR e.ticket_price <= :maxPrice)
            AND (:keyword IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (
                :latitude IS NULL OR :longitude IS NULL OR :radiusMeters IS NULL
                OR ST_DWithin(
                    e.location::geography,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                    :radiusMeters
                )
            )
        ORDER BY 
            CASE WHEN :sortBy = 'distance' AND distance_km IS NOT NULL THEN distance_km END ASC,
            CASE WHEN :sortBy = 'startTime' AND :sortDirection = 'ASC' THEN e.start_time END ASC,
            CASE WHEN :sortBy = 'startTime' AND :sortDirection = 'DESC' THEN e.start_time END DESC,
            CASE WHEN :sortBy = 'createdAt' AND :sortDirection = 'ASC' THEN e.created_at END ASC,
            CASE WHEN :sortBy = 'createdAt' AND :sortDirection = 'DESC' THEN e.created_at END DESC,
            CASE WHEN :sortBy = 'ticketPrice' AND :sortDirection = 'ASC' THEN e.ticket_price END ASC,
            CASE WHEN :sortBy = 'ticketPrice' AND :sortDirection = 'DESC' THEN e.ticket_price END DESC
        """, nativeQuery = true)
    List<Object[]> searchEvents(
            @Param("categoryId") String categoryId,
            @Param("status") String status,
            @Param("verifiedOnly") Boolean verifiedOnly,
            @Param("freeOnly") Boolean freeOnly,
            @Param("availableTicketsOnly") Boolean availableTicketsOnly,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("keyword") String keyword,
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("radiusMeters") Double radiusMeters,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection
    );

    // Count events by host
    long countByHostId(UUID hostId);

    // Count events by category
    long countByCategoryId(UUID categoryId);
}