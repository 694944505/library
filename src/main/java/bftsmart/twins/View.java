package bftsmart.twins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HashMap;

public class View {
    private NodeID leaderID;
    public HashMap<Integer, Integer> leadMap;
    public ArrayList<HashMap<Integer, NodeID>> Partitions;
    
    public View(NodeID LeaderID, ArrayList<HashMap<Integer, NodeID>> arrayList) {
        this.leaderID = LeaderID;
        this.Partitions = arrayList;
        leadMap = new HashMap<Integer, Integer>();
        for(HashMap<Integer, NodeID> partition: Partitions){
            if(partition.containsKey(leaderID.getNetworkID())){
                for(int id: partition.keySet()) {
                    leadMap.put(id, leaderID.getNetworkID());
                }
            } else {
                for(int id: partition.keySet()) {
                    leadMap.put(id, leaderID.getReplicaID());
                }
            } 
        }

    }
    public void setLeaderID(NodeID LeaderID) {
        this.leaderID = LeaderID;
    }
    public int getLeaderID(int networkID) {
        return leadMap.get(networkID);
    }
    public void setPartitions(ArrayList<HashMap<Integer, NodeID>> Partitions) {
        this.Partitions = Partitions;
    }
    public ArrayList<HashMap<Integer, NodeID>> getPartitions() {
        return Partitions;
    }
    @Override
    public String toString() {
        ArrayList<Integer> list = new ArrayList<Integer>();
        for(int i=0;i<Partitions.size();i++){
            for(int id : Partitions.get(i).keySet()) {
                list.add(leadMap.get(id));
                break;
            }  
        }
        String s = "LeaderID: " + list.toString()+ "\nPartitions: \n";
        for (HashMap<Integer, NodeID> partition : Partitions) {
            s += partition.values().toString() + "\n";
        }
        return s;
    }
    public int[] getSamePartitionProcesses(int id) {
        for(HashMap<Integer, NodeID> partition: Partitions){
            if(partition.containsKey(id)){
                int[] array = new int[partition.size()];
                int i = 0;
                for(int id2: partition.keySet()) {
                    array[i] = id2;
                    i++;
                }
                return array;
            }
        }
        return null;
    }
    public boolean isQuick() {
        if (Partitions.size() != 2) return false;
        HashMap<Integer, NodeID> p1 = Partitions.get(0);
        HashMap<Integer, NodeID>p2 = Partitions.get(1);
        if(p1.size() != p2.size()) return false;
        for(int id: p1.keySet()) {
            if(!p1.containsKey(leadMap.get(id))) return false;
        }
        
        for(int id: p2.keySet()) {
            if(!p2.containsKey(leadMap.get(id))) return false;
        }
   
        HashSet<Integer> set1 = new HashSet<Integer>();
        HashSet<Integer> set2 = new HashSet<Integer>();
        for (NodeID node : p1.values()) {
            
            if(!set1.add(node.getReplicaID())) return false;
        }
        for (NodeID node : p2.values()) {
           
            if(!set2.add(node.getReplicaID())) return false;
        }
        return true;
    }
}
