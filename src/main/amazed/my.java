package amazed;

import amazed.maze.Amazed;

public class my {

    public static void main(String[] args) {
        long timeDef = 0; 
        long start = System.currentTimeMillis(); 
        long numberOfRuns = 0;
        
        Amazed amazed = new Amazed("maps/medium.map", false, 3, -1);

        for(int i = 0; i < 100; i++){
            
            amazed.solve();
            numberOfRuns = i; 
        }

        timeDef = System.currentTimeMillis() - start; 

        
        System.out.println("Average Time:"+ timeDef + "in ms" + "Number Of runs" + numberOfRuns);



    }
    
}
