package com.event.entity;

public enum EventStatus {
    PENDING,     // Waiting for admin approval
    APPROVED,    // Approved and visible to users
    CANCELLED,   // Cancelled by host or admin
    COMPLETED    // Event has ended
}