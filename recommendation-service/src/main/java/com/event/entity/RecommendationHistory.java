package com.event.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;


@Entity
@Table(name = "recommendation_history", indexes = {
        @Index(name = "idx_rec_user", columnList = "user_id, recommended_at"),
        @Index(name = "idx_rec_event", columnList = "event_id"),
        @Index(name = "idx_rec_user_event", columnList = "user_id, event_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    // Overall score and ranking
    @Column(nullable = false)
    private Double score;

    @Column(name = "rank_position")
    private Integer rankPosition;

    // Scoring breakdown
    @Column(name = "geo_score")
    private Double geoScore;

    @Column(name = "interest_score")
    private Double interestScore;

    @Column(name = "interaction_score")
    private Double interactionScore;

    @Column(name = "popularity_score")
    private Double popularityScore;

    @Column(name = "recency_score")
    private Double recencyScore;

    // User interaction tracking
    @Column(name = "recommended_at", nullable = false)
    private LocalDateTime recommendedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean clicked = false;

    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean saved = false;

    @Column(name = "saved_at")
    private LocalDateTime savedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean converted = false;  // Bought ticket

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    // Metadata
    @Column(name = "recommendation_reason", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> recommendationReason;

    @Column(name = "algorithm_version", length = 10)
    @Builder.Default
    private String algorithmVersion = "v1.0";

    @Column(name = "distance_km")
    private Double distanceKm;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Mark recommendation as clicked
     */
    public void markAsClicked() {
        this.clicked = true;
        this.clickedAt = LocalDateTime.now();
    }

    /**
     * Mark recommendation as saved
     */
    public void markAsSaved() {
        this.saved = true;
        this.savedAt = LocalDateTime.now();
    }

    /**
     * Mark recommendation as converted (ticket bought)
     */
    public void markAsConverted() {
        this.converted = true;
        this.convertedAt = LocalDateTime.now();
    }
}
