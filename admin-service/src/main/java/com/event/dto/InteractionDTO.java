package com.event.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteractionDTO {
    private UUID id;
    private UUID userId;
    private UUID eventId;
    private String type;  // VIEW, SAVE, SHARE, RSVP, BUY
    private String category;  // Event category for interaction scoring
    private LocalDateTime createdAt;

    /**
     * Check if interaction is a strong signal (SAVE, RSVP, BUY)
     */
    public boolean isStrongSignal() {
        return "SAVE".equals(type) || "RSVP".equals(type) || "BUY".equals(type);
    }

    /**
     * Get weight for this interaction type
     */
    public double getInteractionWeight() {
        return switch (type) {
            case "BUY" -> 1.0;    // Strongest signal
            case "RSVP" -> 0.8;   // Strong signal
            case "SAVE" -> 0.6;   // Medium signal
            case "SHARE" -> 0.4;  // Weak signal
            case "VIEW" -> 0.2;   // Weakest signal
            default -> 0.0;
        };
    }
}
