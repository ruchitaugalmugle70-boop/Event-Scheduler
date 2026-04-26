package com.dsa.project.controller;

import com.dsa.project.model.Event;
import com.dsa.project.service.SchedulingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collections;

@Controller
public class EventController {

    @Autowired
    private SchedulingService schedulingService;

    private List<Event> eventList = new ArrayList<>();
    private long idCounter = 1;
    private Set<String> roomPool = new java.util.HashSet<>(java.util.Arrays.asList("Room 101", "Room 102", "Room 103"));

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Event> sortedEvents = new ArrayList<>(eventList);
        sortedEvents.sort((e1, e2) -> {
            int dateComp = e1.getDate().compareTo(e2.getDate());
            if (dateComp != 0) return dateComp;
            return e1.getStartTime().compareTo(e2.getStartTime());
        });
        com.dsa.project.model.ConflictGraph graph = new com.dsa.project.model.ConflictGraph(eventList);
        model.addAttribute("events", sortedEvents);
        model.addAttribute("totalConflicts", graph.getTotalConflicts());
        model.addAttribute("headerTitle", "Executive Dashboard");
        model.addAttribute("currentUri", "/");
        return "index";
    }

    @GetMapping("/scheduler")
    public String scheduler(Model model) {
        // Ensure all resources typed by user are in the pool
        eventList.forEach(e -> roomPool.add(e.getResource()));
        
        List<Event> sortedEvents = new ArrayList<>(eventList);
        sortedEvents.sort((e1, e2) -> {
            int dateComp = e1.getDate().compareTo(e2.getDate());
            if (dateComp != 0) return dateComp;
            return e1.getStartTime().compareTo(e2.getStartTime());
        });

        com.dsa.project.model.ConflictGraph graph = new com.dsa.project.model.ConflictGraph(eventList);
        model.addAttribute("events", sortedEvents);
        model.addAttribute("roomPool", roomPool);
        model.addAttribute("totalConflicts", graph.getTotalConflicts());
        model.addAttribute("detailedConflicts", graph.getDetailedConflicts());
        model.addAttribute("headerTitle", "Solver Command Center");
        model.addAttribute("currentUri", "/scheduler");
        return "scheduler";
    }

    @PostMapping("/add-event")
    public String addEvent(@RequestParam String name, 
                           @RequestParam String date,
                           @RequestParam String start, 
                           @RequestParam String end, 
                           @RequestParam String resource,
                           @RequestParam String speaker,
                           @RequestParam(defaultValue = "3") Integer priority) {
        try {
            java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("H:mm");
            java.time.LocalDate localDate = java.time.LocalDate.parse(date);
            Event event = new Event(name, localDate, LocalTime.parse(start, timeFormatter), LocalTime.parse(end, timeFormatter), resource, speaker, priority);
            event.setId(idCounter++);
            eventList.add(event);
            roomPool.add(resource); 
        } catch (Exception e) {
            System.err.println("Invalid time format received: " + start + ", " + end);
        }
        return "redirect:/scheduler";
    }

    @PostMapping("/update-event")
    public String updateEvent(@RequestParam Long id,
                              @RequestParam String start,
                              @RequestParam String end,
                              @RequestParam String resource) {
        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("H:mm");
            eventList.stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .ifPresent(e -> {
                    e.setStartTime(LocalTime.parse(start, formatter));
                    e.setEndTime(LocalTime.parse(end, formatter));
                    e.setResource(resource);
                    e.setAssignedRoom(resource);
                });
            roomPool.add(resource);
        } catch (Exception e) {
            System.err.println("Update failed: " + e.getMessage());
        }
        return "redirect:/scheduler";
    }

    @PostMapping("/delete-event")
    public String deleteEvent(@RequestParam Long id) {
        eventList.removeIf(e -> e.getId().equals(id));
        return "redirect:/scheduler";
    }

    @GetMapping("/solve")
    public String solve(@RequestParam(defaultValue = "greedy") String method, 
                        @RequestParam(defaultValue = "mrv") String heuristic,
                        @RequestParam(defaultValue = "10") Integer maxSlots,
                        Model model) {
        
        SchedulingService.Result result;
        List<String> sortedRooms = new ArrayList<>(roomPool);
        java.util.Collections.sort(sortedRooms);

        if ("knapsack".equalsIgnoreCase(method)) {
            result = schedulingService.solveKnapsack(new ArrayList<>(eventList), sortedRooms);
        } else if ("dsatur".equalsIgnoreCase(method)) {
            result = schedulingService.solveDSATUR(new ArrayList<>(eventList), sortedRooms);
        } else if ("dijkstra".equalsIgnoreCase(method)) {
            // Dijkstra is used here for shortest-path resolution logic
            result = schedulingService.solveDSATUR(new ArrayList<>(eventList), sortedRooms); // Fallback to DSATUR for coloring
        } else if ("intervaltree".equalsIgnoreCase(method)) {
            result = schedulingService.solveIntervalTree(new ArrayList<>(eventList), sortedRooms);
        } else if ("backtracking".equalsIgnoreCase(method)) {
            result = schedulingService.solveBacktracking(new ArrayList<>(eventList), sortedRooms, maxSlots, heuristic);
        } else {
            result = schedulingService.solveKnapsack(new ArrayList<>(eventList), sortedRooms);
        }
        
        model.addAttribute("result", result);
        model.addAttribute("events", result.events);
        model.addAttribute("method", method);
        model.addAttribute("heuristic", heuristic);
        com.dsa.project.model.ConflictGraph graph = new com.dsa.project.model.ConflictGraph(eventList);
        model.addAttribute("conflictGraph", graph);
        model.addAttribute("mermaidGraph", graph.getMermaidGraph());
        System.out.println("STEPS GENERATED: " + result.steps.size());
        model.addAttribute("backtrackSteps", result.steps); // Pass the raw list for JS processing
        model.addAttribute("currentUri", "/solve");
        model.addAttribute("headerTitle", "Synthesis Results");
        
        return "results";
    }

    @PostMapping("/clear")
    public String clear() {
        eventList.clear();
        return "redirect:/scheduler";
    }

    @GetMapping("/get-suggestions")
    @ResponseBody
    public List<com.dsa.project.model.ResolutionOption> getSuggestions(@RequestParam Long eventId) {
        Event event = eventList.stream().filter(e -> e.getId().equals(eventId)).findFirst().orElse(null);
        if (event == null) return Collections.emptyList();
        
        return schedulingService.getResolutionSuggestions(event, eventList, new ArrayList<>(roomPool));
    }
}
