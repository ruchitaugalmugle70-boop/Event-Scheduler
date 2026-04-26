package com.dsa.project.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Entity
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String resource; // Original requested room
    private String assignedRoom; // Final allotted room
    private String speaker;  // e.g., "Dr. Smith"
    private Integer priority; // 1 (High) to 5 (Low)
    private Integer colorIndex; // Final allotted time slot

    public Event() {}

    public Event(String name, LocalDate date, LocalTime startTime, LocalTime endTime, String resource, String speaker, Integer priority) {
        this.name = name;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.resource = resource;
        this.assignedRoom = resource;
        this.speaker = speaker;
        this.priority = priority;
        this.colorIndex = -1;
    }

    // Manual Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }

    public String getAssignedRoom() { return assignedRoom; }
    public void setAssignedRoom(String assignedRoom) { this.assignedRoom = assignedRoom; }

    public String getSpeaker() { return speaker; }
    public void setSpeaker(String speaker) { this.speaker = speaker; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public Integer getColorIndex() { return colorIndex; }
    public void setColorIndex(Integer colorIndex) { this.colorIndex = colorIndex; }

    public long getDurationMinutes() {
        int s = startTime.getHour() * 60 + startTime.getMinute();
        int e = endTime.getHour() * 60 + endTime.getMinute();
        if (s > e) e += 1440; // Handles cross-midnight
        return e - s;
    }

    public LocalTime getResolvedStartTime() {
        // Each slot represents a 24-hour shift (Day 1, Day 2, etc.)
        // We return the same LocalTime, but the UI will display 'Slot #X' 
        // to represent which 'Day' the event is on.
        return startTime;
    }

    public String getDisplayDay() {
        if (date != null && (colorIndex == null || colorIndex <= 0)) {
            return date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        }
        if (colorIndex == null || colorIndex <= 0) return "Day 1";
        return "Day " + (colorIndex + 1);
    }

    public LocalTime getResolvedEndTime() {
        return endTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return id != null && id.equals(event.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    /**
     * Logic for showing conflict details in UI
     */
    public boolean conflictsWith(Event other) {
        if (this.id.equals(other.id)) return false;
        
        // DSA Logic: No conflict if on different dates
        if (this.date != null && other.date != null && !this.date.equals(other.date)) return false;
        
        int s1 = this.startTime.getHour() * 60 + this.startTime.getMinute();
        int e1 = this.endTime.getHour() * 60 + this.endTime.getMinute();
        int s2 = other.startTime.getHour() * 60 + other.startTime.getMinute();
        int e2 = other.endTime.getHour() * 60 + other.endTime.getMinute();

        boolean aWraps = s1 >= e1 && !(s1 == e1);
        boolean bWraps = s2 >= e2 && !(s2 == e2);

        boolean temporalOverlap = false;

        if (!aWraps && !bWraps) {
            temporalOverlap = s1 < e2 && s2 < e1;
        } else {
            int[][] aInts = aWraps ? new int[][]{{s1, 1440}, {0, e1}} : new int[][]{{s1, e1}};
            int[][] bInts = bWraps ? new int[][]{{s2, 1440}, {0, e2}} : new int[][]{{s2, e2}};
            outer: for (int[] a : aInts) {
                for (int[] b : bInts) {
                    if (Math.max(a[0], b[0]) < Math.min(a[1], b[1])) {
                        temporalOverlap = true;
                        break outer;
                    }
                }
            }
        }
        
        return temporalOverlap;
    }
}
