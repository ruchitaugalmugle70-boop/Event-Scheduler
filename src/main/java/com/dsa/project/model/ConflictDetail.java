package com.dsa.project.model;

public class ConflictDetail {
    private Event event1;
    private Event event2;
    private String reason;

    public ConflictDetail(Event event1, Event event2, String reason) {
        this.event1 = event1;
        this.event2 = event2;
        this.reason = reason;
    }

    public Event getEvent1() { return event1; }
    public Event getEvent2() { return event2; }
    public String getReason() { return reason; }
}
