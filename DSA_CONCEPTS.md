# Data Structures & Algorithms (DSA) Implementation Report
## Syllabus Focus: Unit II - Greedy Method

This project implements advanced resource optimization using greedy heuristics and pathfinding algorithms as defined in the **Unit II: Greedy Method** syllabus.

---

## 1. Dijkstra’s Algorithm (Shortest Path)
**Syllabus Topic:** Pathfinding & Shortest Path.
- **Why:** In scheduling, finding the minimum displacement for a conflict is a "Shortest Path" problem.
- **Application:** Used for **"Optimal Resolution Pathfinding"**. When a conflict occurs, Dijkstra searches the (Room, Slot) state graph to find the assignment with the absolute minimum cost/priority impact.
- **📍 Location:** `service/SchedulingService.java` -> `solveDijkstra()`

---

## 2. Greedy Knapsack (Fractional/Activity Selection)
**Syllabus Topic:** Fractional Knapsack & Activity Selection.
- **Why:** When room capacity is fixed, we must select events that maximize "Total Importance".
- **Application:** Implemented in the **"Priority Allocation"** mode. It uses the Greedy approach (sorting by Priority/Duration) to select the optimal subset of events.
- **📍 Location:** `service/SchedulingService.java` -> `solveKnapsack()`

---

## 3. Graph Coloring (Greedy & Backtracking)
**Syllabus Topic:** Greedy Method Applications.
- **Why:** Scheduling is a classic **Graph Coloring** problem where two "adjacent" events (those that overlap in time) cannot be assigned the same "color" (Room + Time Slot).
- **Application:** We use greedy heuristics like **Welsh-Powell** and **DSATUR** to assign rooms and slots. This ensures that no two events with a conflict edge share the same resource at the same time.
- **📍 Location:** `model/ConflictGraph.java` and `service/SchedulingService.java`

---

## 4. Interval Tree (Augmented BST)
- **Why:** Efficient conflict detection in $O(\log N)$ for larger datasets.
- **📍 Location:** `model/IntervalTree.java`

---

## 5. Comparison: Greedy vs Divide-and-Conquer
**Syllabus Topic:** Greedy method Concept & Comparison.
- **Greedy (This App):** Makes locally optimal choices at each step (e.g., Dijkstra, Knapsack) to reach a global optimum or a high-quality heuristic solution. It is faster ($O(N \log N)$ typically).
- **Divide-and-Conquer:** Breaks problems into independent sub-problems. In scheduling, D&C is less efficient because assignments are highly dependent on neighbor states.

---

## 6. Summary Table

| Syllabus Topic | Project Implementation | Complexity |
| :--- | :--- | :--- |
| **Dijkstra’s** | Conflict Resolution Path | $O(E \log V)$ |
| **Knapsack** | Priority-based Allocation | $O(N \log N)$ |
| **Greedy Heuristic** | Welsh-Powell & DSATUR | $O(N^2)$ or $O(N \log N)$ |
| **MST Concept** | Conflict Graph backbone | $O(E \log E)$ |
