package com.example.anchornotes.model;

/**
 * Represents a conflict when attempting to set a reminder when one already exists.
 */
public class ReminderConflict {
    public final ReminderType existingType;
    public final ReminderType incomingType;

    public ReminderConflict(ReminderType existingType, ReminderType incomingType) {
        this.existingType = existingType;
        this.incomingType = incomingType;
    }
}
