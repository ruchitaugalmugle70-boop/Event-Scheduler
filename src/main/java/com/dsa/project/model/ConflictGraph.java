package com.dsa.project.model;

import java.util.*;

/**
 * DSA CONCEPT: GRAPH THEORY (Conflict Graph)
 * 
 * Why: Scheduling conflicts are best modeled as a graph where events are vertices 
 * and overlapping constraints are edges. This allows us to use Graph Coloring 
 * algorithms to assign rooms and time slots efficiently.
 */
public class ConflictGraph {
    private List<Event> nodes;
    private Map<Event, Set<Event>> adjacencyList;

    public ConflictGraph(List<Event> events) {
        this.nodes = events;
        this.adjacencyList = new HashMap<>();
        for (Event event : events) {
            adjacencyList.put(event, new HashSet<>());
        }
        buildEdges();
    }

    /**
     * DSA CONCEPT: INTERVAL TREE INTEGRATION
     * 
     * Why: Naive conflict detection is O(N^2). By using an Interval Tree, 
     * we find temporal overlaps in O(N log N) total time for construction 
     * and O(k log N) for queries.
     */
    private void buildEdges() {
        IntervalTree tree = new IntervalTree();
        for (Event event : nodes) tree.insert(event);

        for (Event e1 : nodes) {
            List<Event> overlaps = tree.findOverlaps(e1);
            for (Event e2 : overlaps) {
                if (!e1.equals(e2)) {
                    // Bi-directional constraint
                    adjacencyList.get(e1).add(e2);
                    adjacencyList.get(e2).add(e1);
                }
            }
        }
    }

    public List<Event> getNodes() {
        return nodes;
    }

    public Collection<Event> getNeighbors(Event node) {
        return adjacencyList.getOrDefault(node, Collections.emptySet());
    }

    public int getDegree(Event node) {
        return getNeighbors(node).size();
    }

    public List<ConflictDetail> getDetailedConflicts() {
        List<ConflictDetail> conflicts = new ArrayList<>();
        // Use the adjacency list to avoid O(N^2) double-loop
        Set<String> seenPair = new HashSet<>();
        
        for (Event e1 : nodes) {
            for (Event e2 : getNeighbors(e1)) {
                String pairId = e1.getId() < e2.getId() ? e1.getId()+":"+e2.getId() : e2.getId()+":"+e1.getId();
                if (!seenPair.contains(pairId)) {
                    if (e1.conflictsWith(e2)) {
                        String reason = e1.getResource().equalsIgnoreCase(e2.getResource()) ? "Room Clash" : "Speaker Overlap";
                        conflicts.add(new ConflictDetail(e1, e2, reason));
                    }
                    seenPair.add(pairId);
                }
            }
        }
        return conflicts;
    }

    public String getMermaidGraph() {
        StringBuilder sb = new StringBuilder("graph TD\n");
        sb.append("classDef conflict stroke:#ff4757,stroke-width:2px;\n");
        
        Set<String> addedEdges = new HashSet<>();
        for (Event e1 : nodes) {
            String color = e1.getColorIndex() != -1 ? "color" + e1.getColorIndex() : "default";
            sb.append(String.format("  E%d(\"%s\")\n", e1.getId(), e1.getName()));
            
            for (Event e2 : getNeighbors(e1)) {
                String edgeId = e1.getId() < e2.getId() ? e1.getId()+"-"+e2.getId() : e2.getId()+"-"+e1.getId();
                if (!addedEdges.contains(edgeId)) {
                    sb.append(String.format("  E%d --- E%d\n", e1.getId(), e2.getId()));
                    addedEdges.add(edgeId);
                }
            }
        }
        return sb.toString();
    }

    public int getTotalConflicts() {
        return getDetailedConflicts().size();
    }
}
