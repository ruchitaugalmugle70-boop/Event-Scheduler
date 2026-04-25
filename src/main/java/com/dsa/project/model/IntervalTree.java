package com.dsa.project.model;

import java.util.*;

/**
 * DSA CONCEPT: INTERVAL TREE (Augmented Binary Search Tree)
 * 
 * Why: A standard BST sorts by one value. An Interval Tree augments each node with 
 * a 'max' value (the maximum end time in that subtree). 
 * This allows us to check for overlaps in O(log N) time instead of O(N).
 * 
 * Efficiency:
 * - Insertion: O(log N)
 * - Overlap Search: O(log N + k) where k is the number of overlaps.
 */
public class IntervalTree {
    private IntervalNode root;

    public void insert(Event event) {
        int start = event.getStartTime().getHour() * 60 + event.getStartTime().getMinute();
        int end = event.getEndTime().getHour() * 60 + event.getEndTime().getMinute();

        if (start > end) {
            // Split into [start, 1440] and [0, end] to handle midnight wrap-around
            root = insert(root, new IntervalNode(event, start, 1440));
            root = insert(root, new IntervalNode(event, 0, end));
        } else {
            root = insert(root, new IntervalNode(event, start, end));
        }
    }

    private IntervalNode insert(IntervalNode root, IntervalNode node) {
        if (root == null) return node;

        if (node.start < root.start) {
            root.left = insert(root.left, node);
        } else {
            root.right = insert(root.right, node);
        }

        // Augmented property: store the maximum end time in this subtree
        root.max = Math.max(root.max, node.end);
        return root;
    }

    public List<Event> findOverlaps(Event target) {
        List<Event> overlaps = new ArrayList<>();
        int start = target.getStartTime().getHour() * 60 + target.getStartTime().getMinute();
        int end = target.getEndTime().getHour() * 60 + target.getEndTime().getMinute();

        if (start > end) {
            findOverlaps(root, start, 1440, overlaps, target);
            findOverlaps(root, 0, end, overlaps, target);
        } else {
            findOverlaps(root, start, end, overlaps, target);
        }
        return overlaps;
    }

    private void findOverlaps(IntervalNode root, int start, int end, List<Event> overlaps, Event target) {
        if (root == null) return;

        // Key fix: Ensure we don't count the same event multiple times if it was split
        if (root.start < end && start < root.end) {
            if (root.event != target && !overlaps.contains(root.event)) {
                overlaps.add(root.event);
            }
        }

        // Standard Interval Tree search pruning
        if (root.left != null && root.left.max > start) {
            findOverlaps(root.left, start, end, overlaps, target);
        }

        // Check right subtree
        if (root.right != null && root.start < end) {
            findOverlaps(root.right, start, end, overlaps, target);
        }
    }
}
