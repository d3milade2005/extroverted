package com.event.entity;

public enum InteractionType {
    VIEW,    // User viewed the event
    SAVE,    // User saved/bookmarked the event
    SHARE,   // User shared the event
    RSVP,    // User RSVP'd to the event
    BUY      // User bought a ticket (tracked here + tickets table)
}
