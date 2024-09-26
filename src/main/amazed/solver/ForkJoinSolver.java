package amazed.solver;

import amazed.maze.Maze;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <code>ForkJoinSolver</code> implements a solver for
 * <code>Maze</code> objects using a fork/join multi-threaded
 * depth-first search.
 * <p>
 * Instances of <code>ForkJoinSolver</code> should be run by a
 * <code>ForkJoinPool</code> object.
 */
public class ForkJoinSolver extends SequentialSolver {

    @Override
    protected void initStructures(){
        predecessor = new HashMap<>();
		frontier = new Stack<>();
        visited = new ConcurrentSkipListSet<>(); 
    }

    // List to keep track of all the child solvers created during the search
    List<ForkJoinSolver> childSolvers = new ArrayList<>();

    // Static variable to track if any player has found the goal
   //private static boolean goalFound = false;

    private static AtomicBoolean goalFound = new AtomicBoolean( ); 

    //private  Map<Integer, Integer> predecessor  = new ConcurrentHashMap<>(); 

    List<Integer> res = Collections.synchronizedList(new ArrayList<>());

    static Set<Integer>  visited = new ConcurrentSkipListSet<>(); 

    // Stores the current player assigned to this solver instance
    int currentPlayer;

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal.
     *
     * @param maze the maze to be searched
     */
    public ForkJoinSolver(Maze maze) {
        super(maze);  // Call to the parent constructor
    }

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal, forking after a given number of visited
     * nodes.
     *
     * @param maze      the maze to be searched
     * @param forkAfter the number of steps (visited nodes) after
     *                  which a parallel task is forked; if
     *                  <code>forkAfter <= 0</code> the solver never
     *                  forks new tasks
     */
    public ForkJoinSolver(Maze maze, int forkAfter) {
        this(maze);  // Call the primary constructor
        this.forkAfter = forkAfter;  // Set the number of steps after which to fork
    }

    /**
     * New constructor to initialize the solver with more parameters.
     *
     * @param maze    the maze to be searched
     * @param player  the player ID for this instance
     * @param start   the starting node for the search
     * @param visited the set of visited nodes
     */
    public ForkJoinSolver(Maze maze, int player, int start, Set<Integer> visited) {
        this(maze);  // Call the primary constructor
        this.currentPlayer = player;  // Set the player for this solver instance
        this.start = start;  // Set the start node for this solver
        this.visited = visited;  // Keep track of the visited nodes
        // frontier.push(start); 
        visited.add(player); 
    }

    /**
     * The main method of the solver. This method is called when the
     * solver is executed, and it performs the parallel search.
     *
     * @return the path from the start node to the goal node or
     *         <code>null</code> if no path is found
     */
    @Override
    public List<Integer> compute() {
        // Start the parallel search from the start node with 0 initial steps
        return parallelSearch(start, 0);
    }

    /**
     * Performs the parallel search starting from a given node.
     *
     * @param currentNode the current node being visited
     * @param steps       the number of steps taken so far
     * @return the path to the goal or <code>null</code> if no path is found
     */
    private List<Integer> parallelSearch(int currentNode, int steps) {
        
        // Push the initial node to the frontier (stack) to start visiting it
        frontier.push(currentNode);

        // Loop to visit each node in the frontier (stack)
        while (!this.frontier.isEmpty() && !goalFound.get()) {
            
            // Pop the next node to visit from the frontier
            currentNode = frontier.pop();

            // If this node has already been visited, skip it
            if (!visited.add(currentNode)) {
                continue;
            }

            // Move the current player to the current node
            maze.move(currentPlayer, currentNode);

            // If the current node is a goal, mark the goal as found and return the path
            if (maze.hasGoal(currentNode)) {
                goalFound.set(true);;  // Goal found
                HelperJoiner();  // Join all forked solvers
                return pathFromTo(start, currentNode);  // Return the path from start to the goal
            }
            
            // Prepare to visit the neighbors of the current node
            ArrayList<Integer> neighborsToVisit = new ArrayList<>();

            List<Integer> threadSafeArray = Collections.synchronizedList(neighborsToVisit);

            // Add unvisited neighbors to the list of nodes to visit
            for (Integer nb : maze.neighbors(currentNode)) {
                if (!visited.contains(nb)) 
                {threadSafeArray.add(nb);}
            }

            // For each neighbor, either continue searching or fork a new solver
            for (int i = 0; i < threadSafeArray.size(); i++) {
                int neighbor = threadSafeArray.get(i);
                predecessor.put(neighbor, currentNode);  // Record the predecessor for this neighbor

                // Visit the first neighbor by pushing it to the frontier
                if (i == 0) {
                    frontier.push(neighbor);
                } 
                // Fork a new solver for the remaining neighbors
                else {
                    int newPlayer = maze.newPlayer(neighbor);  // Create a new player for the forked task
                    ForkJoinSolver child = new ForkJoinSolver(maze, newPlayer, neighbor, visited);  // Fork a new solver
                    childSolvers.add(child);  // Add the forked solver to the list of child solvers
                    child.fork();  // Fork the task (run it in parallel)
                }
            }
        }

        // After visiting all nodes, join the results from all forked solvers
        List<Integer> pathToGoal = HelperJoiner();

        // If a path to the goal is found, combine it with the current path
        if (pathToGoal != null) {
            int mid = pathToGoal.remove(0);  // Remove the first node from the path to avoid duplication
            List<Integer> pathFromStart = pathFromTo(start, mid);  // Get the path from the start to the first node
            pathFromStart.addAll(pathToGoal);  // Append the remaining part of the path
            return pathFromStart;
        }

        // If no path is found, return null
        return null;
    }

    /**
     * Helper method to join results from all forked solvers.
     *
     * @return the path to the goal or <code>null</code> if no path is found
     */
    private List<Integer> HelperJoiner() {
        List<Integer> result = null;

        // Join each forked solver and check if it has found a valid path
        for (ForkJoinSolver solver : childSolvers) {
            List<Integer> partialPath = solver.join();  // Join the forked task
            if (partialPath != null) {
                result = partialPath;  // If a path is found, return it
            }
        }

        // Return the result, which may contain the path to the goal
        return result;
    }
    
}
