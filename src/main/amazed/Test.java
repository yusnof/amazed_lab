package amazed;

import amazed.maze.Amazed;

public class Test {

    public static void main(String[] args) {
        double timeDef = 0; 
        double start = System.currentTimeMillis(); 
        System.out.println(start);
        long numberOfRuns = 0;
        double maxRuns = 100000; 
        
        Amazed amazed = new Amazed("maps/medium.map", false, 3, -1);

        for(int i = 0; i <= maxRuns; i++){
            
            amazed.solve();
            numberOfRuns = i; 
        }

        timeDef = System.currentTimeMillis() - start; 
       
        double result = timeDef / maxRuns;
        

        
        System.out.println("Average Time:"+ result + "in ms, " + " number of runs" + numberOfRuns);



    }
    
}
