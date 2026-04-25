package com.dsa.project.model;

public class IntervalNode {
    int start, end, max;
    Event event;
    IntervalNode left, right;

    public IntervalNode(Event event, int start, int end) {
        this.event = event;
        this.start = start;
        this.end = end;
        this.max = end;
    }
}
