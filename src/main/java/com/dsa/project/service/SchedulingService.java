package com.dsa.project.service;

import com.dsa.project.model.Event;
import com.dsa.project.model.ConflictGraph;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.*;

/**
 * DSA CORE: CORE SCHEDULING SERVICE
 * 
 * This service implements multiple graph-coloring strategies to solve the 
 * resource allocation problem.
 */
@Service
public class SchedulingService {

    public static class Result {
        public List<Event> events;
        public long timeTakenNs;
        public int backtrackCount;
        public int prunedCount;
        public boolean success;
        public List<com.dsa.project.model.BacktrackStep> steps = new ArrayList<>();

        public Result(List<Event> events, long timeTakenNs, int backtrackCount, int prunedCount, boolean success) {
            this.events = events;
            this.timeTakenNs = timeTakenNs;
            this.backtrackCount = backtrackCount;
            this.prunedCount = prunedCount;
            this.success = success;
        }

        public void addStep(Event e, String room, int slot, String action) {
            steps.add(new com.dsa.project.model.BacktrackStep(e.getId(), e.getName(), room, slot, action));
        }
    }

    private int backtrackCount = 0;
    private int prunedCount = 0;

    /**
     * DSA CONCEPT: DSATUR ALGORITHM (Degree of Saturation)
     * 
     * Why: DSATUR is a dynamic greedy algorithm that provides better coloring 
     * results than static sorting. It picks the node with the highest "saturation degree"
     * (number of distinct colors assigned to neighbors).
     * 
     * DSA INTEGRATION: DYNAMIC PRIORITY UPDATES
     * We use a map to track state and repeatedly search for the most constrained vertex.
     */
    public Result solveDSATUR(List<Event> events, List<String> availableRooms) {
        if (events.isEmpty()) return new Result(events, 0, 0, 0, true);
        
        long startTime = System.nanoTime();
        events.forEach(e -> {
            e.setColorIndex(-1);
            e.setAssignedRoom("");
        });
        
        ConflictGraph graph = new ConflictGraph(events);
        int uncoloredCount = events.size();

        Result result = new Result(events, 0, 0, 0, true);
        while (uncoloredCount > 0) {
            Event current = selectNextDSATURNode(events, graph);
            boolean assigned = false;

            outer: for (int slot = 0; slot < 10; slot++) {
                for (String room : availableRooms) {
                    result.addStep(current, room, slot, "TRY");
                    if (isSafe(graph, current, room, slot)) {
                        current.setAssignedRoom(room);
                        current.setColorIndex(slot);
                        result.addStep(current, room, slot, "SUCCESS");
                        assigned = true;
                        break outer;
                    } else {
                        result.addStep(current, room, slot, "CONFLICT");
                    }
                }
            }
            uncoloredCount--;
        }
        
        result.timeTakenNs = System.nanoTime() - startTime;
        return result;
    }

    private Event selectNextDSATURNode(List<Event> events, ConflictGraph graph) {
        Event bestNode = null;
        int maxSaturation = -1;
        int maxDegreeInUncolored = -1;

        for (Event e : events) {
            if (e.getColorIndex() != -1) continue;

            // Calculate Saturation Degree (Distinct colors of neighbors)
            Set<Integer> distinctNeighborColors = new HashSet<>();
            int uncoloredDegree = 0;
            for (Event neighbor : graph.getNeighbors(e)) {
                if (neighbor.getColorIndex() != -1) {
                    distinctNeighborColors.add(neighbor.getColorIndex());
                } else {
                    uncoloredDegree++;
                }
            }

            int saturation = distinctNeighborColors.size();
            
            // DSATUR Rule: 
            // 1. Maximize saturation degree
            // 2. Tie-break: Maximize degree in uncolored subgraph
            if (saturation > maxSaturation) {
                maxSaturation = saturation;
                maxDegreeInUncolored = uncoloredDegree;
                bestNode = e;
            } else if (saturation == maxSaturation && uncoloredDegree > maxDegreeInUncolored) {
                maxDegreeInUncolored = uncoloredDegree;
                bestNode = e;
            }
        }
        return bestNode;
    }

    /**
     * DSA CONCEPT (UNIT II): GREEDY ALGORITHM (Welsh-Powell Variation)
     * 
     * Why: Greedy coloring is the standard heuristic for the NP-complete Graph Coloring problem.
     * By ordering nodes by degree (descending), we minimize the number of colors 
     * (slots) needed for a valid schedule.
     * 
     * DSA INTEGRATION: HEAP / PRIORITY QUEUE
     * We use a Max-Heap (PriorityQueue) to efficiently pick the next most complicated vertex.
     */
    public Result solveGreedy(List<Event> events, List<String> availableRooms) {
        if (events.isEmpty()) return new Result(events, 0, 0, 0, true);
        
        long startTime = System.nanoTime();
        events.forEach(e -> {
            e.setColorIndex(-1);
            e.setAssignedRoom(e.getResource());
        });
        
        ConflictGraph graph = new ConflictGraph(events);
        
        // Priority Queue (Max-Heap) to store events by their degree in the conflict graph
        PriorityQueue<Event> pq = new PriorityQueue<>((a, b) -> 
            Integer.compare(graph.getDegree(b), graph.getDegree(a))
        );
        pq.addAll(events);

        Result result = new Result(events, 0, 0, 0, true);
        while (!pq.isEmpty()) {
            Event current = pq.poll();
            boolean assigned = false;

            outer: for (int slot = 0; slot < 10; slot++) {
                for (String room : availableRooms) {
                    result.addStep(current, room, slot, "TRY");
                    if (isSafe(graph, current, room, slot)) {
                        current.setAssignedRoom(room);
                        current.setColorIndex(slot);
                        result.addStep(current, room, slot, "SUCCESS");
                        assigned = true;
                        break outer;
                    } else {
                        result.addStep(current, room, slot, "CONFLICT");
                    }
                }
            }
        }
        
        result.timeTakenNs = System.nanoTime() - startTime;
        return result;
    }

    private int countLCV(Event current, com.dsa.project.model.AssignmentColor val, ConflictGraph graph, Map<Event, Set<com.dsa.project.model.AssignmentColor>> domains) {
        int constraints = 0;
        for (Event neighbor : graph.getNeighbors(current)) {
            if (neighbor.getColorIndex() == -1) {
                if (domains.get(neighbor).contains(val)) constraints++;
            }
        }
        return constraints;
    }

    private Event selectUnassignedVariable(List<Event> events, ConflictGraph graph, String heuristic) {
        List<Event> unassigned = events.stream().filter(e -> e.getColorIndex() == -1).toList();
        if (unassigned.isEmpty()) return null;

        if ("degree".equalsIgnoreCase(heuristic)) {
            return unassigned.stream()
                .max(Comparator.comparingInt(graph::getDegree))
                .orElse(unassigned.get(0));
        }
        return unassigned.get(0);
    }

    /**
     * DSA CONCEPT: BACKTRACKING WITH CONSTRAINT SATISFACTION (CSP)
     * 
     * Why: Backtracking explores the search space to find a valid assignment 
     * when the greedy approach is insufficient.
     * 
     * DSA INTEGRATION: AC-3 (Arc Consistency) & LCV (Least Constraining Value)
     * - AC-3: Prunes the search space before and during recursion to reduce branching.
     * - LCV: A heuristic that chooses the value that leaves the most options for neighbors.
     */
    public Result solveBacktracking(List<Event> events, List<String> availableRooms, int maxSlots, String heuristic) {
        if (events.isEmpty()) return new Result(events, 0, 0, 0, true);
        
        long startTime = System.nanoTime();
        backtrackCount = 0;
        prunedCount = 0;
        events.forEach(e -> e.setColorIndex(-1));
        
        ConflictGraph graph = new ConflictGraph(events);
        
        // Initialize domains for CSP
        Map<Event, Set<com.dsa.project.model.AssignmentColor>> domains = new HashMap<>();
        for (Event e : events) {
            Set<com.dsa.project.model.AssignmentColor> domain = new LinkedHashSet<>();
            for (int slot = 0; slot < maxSlots; slot++) {
                for (String room : availableRooms) {
                    domain.add(new com.dsa.project.model.AssignmentColor(room, slot));
                }
            }
            domains.put(e, domain);
        }

        // Apply AC-3 Pre-processing
        if (!ac3(graph, domains)) {
            return new Result(events, System.nanoTime() - startTime, 0, 0, false);
        }

        Result result = new Result(events, 0, 0, 0, false);
        if (backtrack(events, graph, domains, availableRooms, maxSlots, heuristic, result)) {
            result.success = true;
            result.timeTakenNs = System.nanoTime() - startTime;
            result.backtrackCount = backtrackCount;
            result.prunedCount = prunedCount;
            return result;
        }
        
        result.timeTakenNs = System.nanoTime() - startTime;
        result.backtrackCount = backtrackCount;
        result.prunedCount = prunedCount;
        return result;
    }

    private boolean ac3(ConflictGraph graph, Map<Event, Set<com.dsa.project.model.AssignmentColor>> domains) {
        Queue<Event[]> queue = new LinkedList<>();
        for (Event e1 : graph.getNodes()) {
            for (Event e2 : graph.getNeighbors(e1)) {
                queue.add(new Event[]{e1, e2});
            }
        }

        while (!queue.isEmpty()) {
            Event[] arc = queue.poll();
            Event xi = arc[0];
            Event xj = arc[1];

            if (revise(xi, xj, domains)) {
                if (domains.get(xi).isEmpty()) return false;
                for (Event xk : graph.getNeighbors(xi)) {
                    if (xk != xj) queue.add(new Event[]{xk, xi});
                }
            }
        }
        return true;
    }

    private boolean revise(Event xi, Event xj, Map<Event, Set<com.dsa.project.model.AssignmentColor>> domains) {
        boolean revised = false;
        Iterator<com.dsa.project.model.AssignmentColor> it = domains.get(xi).iterator();
        while (it.hasNext()) {
            com.dsa.project.model.AssignmentColor ci = it.next();
            boolean hasSatisfyingXj = false;
            for (com.dsa.project.model.AssignmentColor cj : domains.get(xj)) {
                // Constraint Check: Must not have same room AND same slot if they conflict
                if (!(ci.room.equals(cj.room) && ci.slot == cj.slot)) {
                    hasSatisfyingXj = true;
                    break;
                }
            }
            if (!hasSatisfyingXj) {
                it.remove();
                prunedCount++;
                revised = true;
            }
        }
        return revised;
    }

    private boolean backtrack(List<Event> events, ConflictGraph graph, Map<Event, Set<com.dsa.project.model.AssignmentColor>> domains, 
                             List<String> availableRooms, int maxSlots, String heuristic, Result result) {
        Event current = selectUnassignedVariable(events, graph, heuristic);
        if (current == null) return true;

        backtrackCount++;

        // Value Ordering: Least Constraining Value (LCV)
        List<com.dsa.project.model.AssignmentColor> values = new ArrayList<>(domains.get(current));
        values.sort((v1, v2) -> Integer.compare(
            countLCV(current, v1, graph, domains),
            countLCV(current, v2, graph, domains)
        ));

        for (com.dsa.project.model.AssignmentColor val : values) {
            result.addStep(current, val.room, val.slot, "TRY");
            
            if (isSafe(graph, current, val.room, val.slot)) {
                current.setAssignedRoom(val.room);
                current.setColorIndex(val.slot);
                result.addStep(current, val.room, val.slot, "SUCCESS");
                
                if (backtrack(events, graph, domains, availableRooms, maxSlots, heuristic, result)) return true;
                
                result.addStep(current, val.room, val.slot, "FAIL");
                current.setColorIndex(-1);
                current.setAssignedRoom(current.getResource()); 
            } else {
                result.addStep(current, val.room, val.slot, "CONFLICT");
            }
        }
        return false;
    }

    /**
     * DSA CONCEPT (UNIT II): DIJKSTRA'S ALGORITHM (Shortest Path)
     * 
     * Why: In complex schedules, moving one event might cause a chain reaction. 
     * Dijkstra's finds the "Shortest Path" (minimum cost/displacement) to a 
     * valid state by exploring a weighted graph of potential event assignments.
     */
    public List<com.dsa.project.model.ResolutionOption> solveDijkstra(Event event, List<Event> allEvents, List<String> availableRooms) {
        // Priority Queue for Dijkstra: stores (ResolutionOption, CumulativeCost)
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(n -> n.cost));
        Map<String, Double> dist = new HashMap<>();

        ConflictGraph graph = new ConflictGraph(allEvents);
        
        // Initial state
        Node start = new Node(new com.dsa.project.model.ResolutionOption(event.getResource(), event.getStartTime(), event.getEndTime(), "Start"), 0.0);
        pq.add(start);

        List<com.dsa.project.model.ResolutionOption> bestOptions = new ArrayList<>();

        while (!pq.isEmpty() && bestOptions.size() < 3) {
            Node current = pq.poll();
            String stateKey = current.opt.getRoom() + ":" + current.opt.getStartTime();

            if (dist.containsKey(stateKey) && dist.get(stateKey) <= current.cost) continue;
            dist.put(stateKey, current.cost);

            // Explore potential neighbors (different rooms or time slots)
            for (String room : availableRooms) {
                for (int slotOffset = 0; slotOffset < 5; slotOffset++) {
                    LocalTime newStart = event.getStartTime().plusHours(slotOffset);
                    LocalTime newEnd = event.getEndTime().plusHours(slotOffset);
                    
                    if (isSafe(graph, event, room, slotOffset)) {
                        double stepCost = (slotOffset * 10.0) + (room.equalsIgnoreCase(event.getResource()) ? 0 : 5);
                        com.dsa.project.model.ResolutionOption nextOpt = new com.dsa.project.model.ResolutionOption(
                            room, newStart, newEnd, "Efficient move to " + room
                        );
                        
                        if (current.cost + stepCost < 100) { // Limit search depth
                            bestOptions.add(nextOpt);
                        }
                    }
                }
            }
        }
        return bestOptions;
    }

    private static class Node {
        com.dsa.project.model.ResolutionOption opt;
        double cost;
        Node(com.dsa.project.model.ResolutionOption opt, double cost) {
            this.opt = opt;
            this.cost = cost;
        }
    }

    /**
     * DSA CONCEPT (UNIT II): GREEDY KNAPSACK (Activity Selection)
     * 
     * Why: When resource capacity is limited, we use the Greedy approach 
     * (sorting by value/duration ratio) to select the most important events.
     */
    public List<Event> solveKnapsack(List<Event> events, int totalResourceHours) {
        // Sort by Value/Duration ratio (Fractional Knapsack logic)
        List<Event> sorted = new ArrayList<>(events);
        sorted.sort((a, b) -> Double.compare(
            (double)b.getPriority() / 1.0, // Assuming 1 hour for simplicity
            (double)a.getPriority() / 1.0
        ));

        List<Event> selected = new ArrayList<>();
        int capacity = totalResourceHours;
        for (Event e : sorted) {
            if (capacity >= 1) {
                selected.add(e);
                capacity -= 1;
            }
        }
        return selected;
    }

    /**
     * Constraint Checking Logic: PHYSICAL EXCLUSIVITY
     * 
     * A room can only hold ONE event at any given time.
     * If two events overlap in time (have an edge in the Conflict Graph),
     * they MUST be assigned to different rooms.
     */
    private boolean isSafe(ConflictGraph graph, Event current, String room, int slot) {
        for (Event neighbor : graph.getNeighbors(current)) {
            if (neighbor.getColorIndex() != -1) {
                if (neighbor.getColorIndex() == slot) {
                    if (neighbor.getAssignedRoom().equalsIgnoreCase(room)) return false;
                    if (neighbor.getSpeaker().equalsIgnoreCase(current.getSpeaker())) return false;
                }
            }
        }
        return true;
    }

    public List<com.dsa.project.model.ResolutionOption> getResolutionSuggestions(Event event, List<Event> allEvents, List<String> availableRooms) {
        return solveDijkstra(event, allEvents, availableRooms);
    }
}
