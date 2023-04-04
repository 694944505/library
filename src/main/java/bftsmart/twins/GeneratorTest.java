package bftsmart.twins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class GeneratorTest {
    
    
    public static void TestGenerator() {
        Generator g = new Generator(new Settings(4, 2, 2, 12), LoggerFactory.getLogger("generator_test"));
        g.Shuffle(0);
        g.setQuik();
        System.out.println("possible Partitions: " );

        for(ArrayList<HashMap<Integer, NodeID>> partitions: g.getPartitionScenarios()){
            for(HashMap<Integer, NodeID> partition: partitions){
                System.out.print(partition.toString()+"\n");
            }
            System.out.println("\n\n");
        }
        
        for(int i=0;i<12;i++){
                View v =g.getView(i);
            if(v.isQuick())
                System.out.println(i+g.getView(i).toString());
                
            
        }
    }
    public static void main(String[] args) throws Exception{
        try{
            TestGenerator();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}
