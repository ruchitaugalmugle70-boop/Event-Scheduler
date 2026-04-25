package com.dsa.project.model;

import java.time.LocalTime;

public class ResolutionOption {
    private String room;
    private LocalTime startTime;
    private LocalTime endTime;
    private String description;

    public ResolutionOption(String room, LocalTime startTime, LocalTime endTime, String description) {
        this.room = room;
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
    }

    public String getRoom() { return room; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getDescription() { return description; }
}
