package com.dsa.project.model;

public class BacktrackStep {
    private Long eventId;
    private String eventName;
    private String room;
    private int slot;
    private String action; // "TRY", "SUCCESS", "FAIL", "CONFLICT"

    public BacktrackStep() {}

    public BacktrackStep(Long eventId, String eventName, String room, int slot, String action) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.room = room;
        this.slot = slot;
        this.action = action;
    }

    public Long getEventId() { return eventId; }
    public String getEventName() { return eventName; }
    public String getRoom() { return room; }
    public int getSlot() { return slot; }
    public String getAction() { return action; }
}
