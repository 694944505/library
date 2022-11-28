package bftsmart.twins;

import java.util.ArrayList;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class GeneratorTest {
    
    
    public static void TestGenerator() {
        Generator g = new Generator(new Settings(4, 1, 3, 80), LoggerFactory.getLogger("generator_test"));
        g.Shuffle(0);
        System.out.println("possible Partitions: " );
        for(ArrayList<HashSet<Integer>> partitions: g.getPartitionScenarios()){
            for(HashSet<Integer> partition: partitions){
                System.out.print(partition.toString()+", ");
            }
            System.out.println("");
        }
        for(View v: g.getNextScenario()){
            System.out.println(v.toString());
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
