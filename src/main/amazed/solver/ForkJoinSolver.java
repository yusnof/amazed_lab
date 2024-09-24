package amazed.solver;

import amazed.maze.Maze;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * <code>ForkJoinSolver</code> implements a solver for
 * <code>Maze</code> objects using a fork/join multi-thread
 * depth-first search.
 * <p>
 * Instances of <code>ForkJoinSolver</code> should be run by a
 * <code>ForkJoinPool</code> object.
 */

public class ForkJoinSolver
        extends SequentialSolver {

    List<ForkJoinSolver> childerSolver = new ArrayList<>(); 
    private static boolean GoalFound = false;
    int currentPlayer; 

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal.
     *
     * @param maze the maze to be searched
     */
    public ForkJoinSolver(Maze maze) {
        super(maze);
    }

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal, forking after a given number of visited
     * nodes.
     *
     * @param maze      the maze to be searched
     * @param forkAfter the number of steps (visited nodes) after
     *                  which a parallel task is forked; if
     *                  <code>forkAfter &lt;= 0</code> the solver never
     *                  forks new tasks
     */
    public ForkJoinSolver(Maze maze, int forkAfter) {
        this(maze);
        this.forkAfter = forkAfter;
    }

    // new constructor

    public ForkJoinSolver(Maze maze, int player, int start, Set<Integer> visited) {
        this(maze);
        this.currentPlayer = player;
        this.start = start;
        this.visited = visited; 
    }

    /**
     * Searches for and returns the path, as a list of node
     * identifiers, that goes from the start node to a goal node in
     * the maze. If such a path cannot be found (because there are no
     * goals, or all goals are unreacheable), the method returns
     * <code>null</code>.
     *
     * @return the list of node identifiers from the start node to a
     *         goal node in the maze; <code>null</code> if such a path cannot
     *         be found.
     */
    @Override
    public List<Integer> compute() {
        // we compute from start node and asign steps to 0 
        return parallelSearch(start, 0);
    }

    private List<Integer> parallelSearch(int currentNode, int steps) {
        
        //we push the first node to the stack to be visited 
        frontier.push(currentNode);

        // look throw stack in order to visi all of the niebors 
        while (!frontier.isEmpty() && !GoalFound) {
            
            currentNode = frontier.pop();

            if (!visited.add(currentNode)) {
                continue;
            }
            maze.move(currentPlayer, currentNode);

            if (maze.hasGoal(currentNode)) {
                GoalFound = true;
                HelperJoiner();
                return pathFromTo(start, currentNode);
            }
            
            ArrayList<Integer> nieghborsToVisit = new ArrayList<>();

            for (Integer nb : maze.neighbors(currentNode)) {
                if (!visited.contains(nb)) nieghborsToVisit.add(nb);
            }
            for (int i = 0; i < nieghborsToVisit.size(); i++) {
                int nieghbors = nieghborsToVisit.get(i);
                predecessor.put(nieghbors, currentNode);

                if (i == 0) {
                    frontier.push(nieghbors);
                } else {
                    int newPlayer = maze.newPlayer(nieghbors);
                    ForkJoinSolver child = new ForkJoinSolver(maze, newPlayer, nieghbors, visited);
                    childerSolver.add(child);
                    child.fork();
                }
            }
        }
        List<Integer> pathToGoal = HelperJoiner();

        if (pathToGoal != null) {
            int mid = pathToGoal.remove(0);
            List<Integer> pathFromStart = pathFromTo(start, mid);
            pathFromStart.addAll(pathToGoal);
            return pathFromStart;
        }
        return null;
        
    
    }

    
    private List<Integer> HelperJoiner() {
        List<Integer> result = null;

        for (ForkJoinSolver solver : childerSolver) {
            List<Integer> partialPath = solver.join();
            if (partialPath != null) {
                result = partialPath;}
        }
        return result;
    }
}