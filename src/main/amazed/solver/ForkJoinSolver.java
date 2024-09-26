package amazed.solver;

import amazed.maze.Maze;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import java.util.Set;
import java.util.Stack;

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
        return parallelSearch();
    }

    /**
     * Performs the parallel search starting from a given node.
     *
     * @param currentNode the current node being visited
     * @param steps       the number of steps taken so far
     * @return the path to the goal or <code>null</code> if no path is found
     */
    private List<Integer> parallelSearch()
	{
		
		int step = 0;
		
	    currentPlayer = maze.newPlayer(start);
		
		frontier.push(start);
		
			
			while (!frontier.empty() && !goalFound.get()) {
				
				int currentNode = frontier.pop();
				
				if (visited.add(currentNode)||currentNode==start) {
					
					if (maze.hasGoal(currentNode)) {
						
						goalFound.set(true);
						
						maze.move(currentPlayer, currentNode);
						
						step++;
						return pathFromTo(start, currentNode);
					}

					
					maze.move(currentPlayer, currentNode);
					
					step++;
					boolean onWay = true;
					for (int neighbor: maze.neighbors(currentNode)) {

						
						if(!visited.contains(neighbor)) {
							predecessor.put(neighbor, currentNode);
							
							if (onWay || step<forkAfter) {
								
								frontier.push(neighbor);
								onWay=false;
							}
							
							else {
								if(visited.add(neighbor)){
									step=0;
									ForkJoinSolver childSolver = new ForkJoinSolver(maze,forkAfter,neighbor,visited);
									childSolvers.add(childSolver);
									childSolver.fork();
								}
							}
						}
					}
				}
			}
			// all nodes explored, no goal found
		
			return HelperJoiner();
		}

    /**
     * Helper method to join results from all forked solvers.
     *
     * @return the path to the goal or <code>null</code> if no path is found
     */
    private List<Integer> HelperJoiner() {
        
        for (ForkJoinSolver child:childSolvers) {
			List<Integer> result = child.join();
			if(result!=null) {
				List<Integer> myPath = pathFromTo(start, predecessor.get(child.start));
				myPath.addAll(result);
				return myPath;
			}
		}
		return null;
	}
    
}
