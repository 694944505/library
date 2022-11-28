package bftsmart.twins;

import java.util.ArrayList;
import java.util.HashSet;

public class View {
    private int leaderID;
    private ArrayList<HashSet<Integer>> Partitions;
    
    public View(int LeaderID, ArrayList<HashSet<Integer>> arrayList) {
        this.leaderID = LeaderID;
        this.Partitions = arrayList;

    }
    public void setLeaderID(int LeaderID) {
        this.leaderID = LeaderID;
    }
    public int getLeaderID() {
        return leaderID;
    }
    public void setPartitions(ArrayList<HashSet<Integer>> Partitions) {
        this.Partitions = Partitions;
    }
    public ArrayList<HashSet<Integer>> getPartitions() {
        return Partitions;
    }
    @Override
    public String toString() {
        String s = "LeaderID: " + leaderID + " Partitions: ";
        for (HashSet<Integer> partition : Partitions) {
            s += partition.toString() + " ";
        }
        return s;
    }
}
