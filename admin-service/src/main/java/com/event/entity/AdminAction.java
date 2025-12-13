package com.event.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;


@Entity
@Table(name = "admin_actions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;

    @Column(name = "performed_by", nullable = false)
    private UUID performedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_entity_type", nullable = false, length = 50)
    private EntityType targetEntityType;

    @Column(name = "target_entity_id", nullable = false)
    private UUID targetEntityId;

    @Column(name = "action_description", columnDefinition = "TEXT")
    private String actionDescription;

    /**
     * State of entity before action (as JSON)
     * Example: {"status": "pending", "title": "Old Title"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_state", columnDefinition = "jsonb")
    private Map<String, Object> previousState;

    /**
     * State of entity after action (as JSON)
     * Example: {"status": "approved", "title": "Old Title"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_state", columnDefinition = "jsonb")
    private Map<String, Object> newState;

    /**
     * Additional metadata about the action (as JSON)
     * Example: {"reason": "Violates community guidelines", "notified": true}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "action_metadata", columnDefinition = "jsonb")
    private Map<String, Object> actionMetadata;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Get a human-readable description of this action
     */
    public String getReadableDescription() {
        if (actionDescription != null && !actionDescription.isEmpty()) {
            return actionDescription;
        }

        return String.format("%s performed %s on %s (ID: %s)",
                performedBy,
                actionType.getDisplayName(),
                targetEntityType.getDisplayName(),
                targetEntityId
        );
    }

    /**
     * Get metadata value by key
     */
    public Object getMetadata(String key) {
        return actionMetadata != null ? actionMetadata.get(key) : null;
    }

    /**
     * Check if this action modified the entity
     */
    public boolean isModification() {
        return previousState != null && newState != null && !previousState.equals(newState);
    }

    /**
     * Get the reason for this action from metadata
     */
    public String getReason() {
        Object reason = getMetadata("reason");
        return reason != null ? reason.toString() : null;
    }
}