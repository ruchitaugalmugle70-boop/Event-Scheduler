package com.dsa.project.model;

import java.util.Objects;

public class AssignmentColor {
    public final String room;
    public final int slot;

    public AssignmentColor(String room, int slot) {
        this.room = room;
        this.slot = slot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssignmentColor that = (AssignmentColor) o;
        return slot == that.slot && Objects.equals(room, that.room);
    }

    @Override
    public int hashCode() {
        return Objects.hash(room, slot);
    }
}
