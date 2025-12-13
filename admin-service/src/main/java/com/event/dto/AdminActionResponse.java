package com.event.dto;

import com.cityvibe.admin.model.ActionType;
import com.cityvibe.admin.model.AdminAction;
import com.cityvibe.admin.model.EntityType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class AdminActionResponse {

    private UUID id;
    private ActionType actionType;
    private UUID performedBy;
    private EntityType targetEntityType;
    private UUID targetEntityId;
    private String actionDescription;
    private Map<String, Object> actionMetadata;
    private LocalDateTime createdAt;

    public static AdminActionResponse from(AdminAction action) {
        return AdminActionResponse.builder()
                .id(action.getId())
                .actionType(action.getActionType())
                .performedBy(action.getPerformedBy())
                .targetEntityType(action.getTargetEntityType())
                .targetEntityId(action.getTargetEntityId())
                .actionDescription(action.getActionDescription())
                .actionMetadata(action.getActionMetadata())
                .createdAt(action.getCreatedAt())
                .build();
    }
}