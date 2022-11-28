package bftsmart.twins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

import org.slf4j.Logger;
// Generator generates twins scenarios.
public class Generator {
    private long remaining;
    private ArrayList<NodeID> allNodes;
    private ArrayList<NodeID> twins;
    private ArrayList<NodeID> nodes;
    private int[] indices;
    private int[] offsets;
    private ArrayList<View> leadersPartitions;
    private ArrayList<ArrayList<HashSet<Integer>>> partitionScenarios;
    private Settings settings;
    private Logger logger;
    private final Object mut = new Object();
   
    public Generator(Settings settings, Logger logger) {
        this.settings = settings;
        this.logger = logger;
        
        this.allNodes = new ArrayList<NodeID>();
        this.twins = new ArrayList<NodeID>();
        this.nodes = new ArrayList<NodeID>();
        this.indices = new int[settings.getViews()];
        this.offsets = new int[settings.getViews()];
        leadersPartitions = new ArrayList<View>();
        assignNodeIDs();

        allNodes.addAll(twins);
        allNodes.addAll(nodes);

        partitionScenarios = genPartitionScenarios();

        for (int i = 0; i < partitionScenarios.size(); i++) {
            for (int j = 0; j < nodes.size(); j++) {
                leadersPartitions.add(new View(nodes.get(j).getReplicaID(), partitionScenarios.get(i)));
            }
        }   
        remaining = (long) Math.pow(leadersPartitions.size(), settings.getViews());
        logger.info(remaining + " scenarios can be generated with current settings.");
    }
    
    // Shuffle shuffles the list of leaders and partitions.
    public void Shuffle(long seed) {
        settings.setShuffle(true);
        settings.setSeed(seed);
        
        Random r = new Random(seed);
        Collections.shuffle(leadersPartitions);
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = r.nextInt(leadersPartitions.size());
        }
    }

    // NextScenario generates the next scenario.
    public View[] getNextScenario(){
        //TODO locks

        View[] scenarios = new View[settings.getViews()];
        // get the partition scenarios for this scenario
        for (int i = 0; i < indices.length; i++) {
            // randomize the selection somewhat by adding in the offsets generated by the Shuffle method
            int index = indices[i] + offsets[i];
            if (index >= leadersPartitions.size()) {
                index -= leadersPartitions.size();
            }
            scenarios[i] = leadersPartitions.get(index);
        }
        // This is basically computing the cartesian product of leadersPartitions with itself "round" times.
        for (int i = settings.getViews() - 1; i >= 0; i--) {
            indices[i]++;
            if (indices[i] < leadersPartitions.size()) {
                break;
            }
            indices[i] = 0;
            if (i <= 0) {
                indices = new int[0];
                return null;
            }
        }
        remaining--;
        return scenarios;
    }
    
    public void assignNodeIDs() {
        int replicaID = 1;
        int networkID = 1;
        int remainingTwins = settings.getNumTwins();
        // assign IDs to nodes
        for (int i = 0; i < settings.getNumNodes(); i++) {
            if (remainingTwins > 0) {
                twins.add(new NodeID(replicaID, networkID));
                networkID++;
                twins.add(new NodeID(replicaID, networkID));
                remainingTwins--;
            } else {
                nodes.add(new NodeID(replicaID, networkID));
            }
            networkID++;
            replicaID++;
        }
        for(NodeID node: twins){
            //logger.debug("Twin: " + node.toString());
        }
        for(NodeID node: nodes){
            //logger.debug("Node: " + node.toString());
        }
    }

    public ArrayList<ArrayList<HashSet<Integer>>> genPartitionScenarios() {
        ArrayList<ArrayList<HashSet<Integer>>> partitionScenarios = new ArrayList<ArrayList<HashSet<Integer>>>();
        int n = allNodes.size();
        ArrayList<ArrayList<int []>> twinAssignments = new ArrayList<ArrayList<int []>>();
        // generate all ways to assign the twins to k partitions
        if (twins.size() / 2 > 0) {
            twinAssignments.add(generateTwinPartitionPairs(settings.getPartitions()));
            for (int i = 1; i < twins.size() / 2; i++) {
                twinAssignments.add(twinAssignments.get(0));
            }
            twinAssignments = cartesianProduct(twinAssignments);
            
        }
        
        ArrayList<int []> sizes = genPartitionSizes(n, settings.getPartitions(), 1);

        for(int i = 0; i < sizes.size(); i++) {
            int j = 0;
            for(boolean ok = true; ok; ok = j < twinAssignments.size()) {

                if (twinAssignments.size() > 0 && !isValidTwinAssignment(twinAssignments.get(j), sizes.get(i))) {
                    j++;
                    continue;
                }
                ArrayList<HashSet<Integer>> partitions = new ArrayList<HashSet<Integer>>();
                for(int k = 0; k < sizes.get(i).length; k++) {
                    if (sizes.get(i)[k] > 0) {
                        partitions.add(new HashSet<Integer>());
                    }
                }
                if (twinAssignments.size() > 0) {
                    int twin = 0;
                    for(int k = 0; k < twinAssignments.get(j).size(); k++) {
                        for(int t = 0; t < twinAssignments.get(j).get(k).length; t++) {
                            partitions.get(twinAssignments.get(j).get(k)[t]).add(twins.get(twin).getNetworkID());
                            twin++;
                        }
                    }
                }
                int node = 0;
                for(int k = 0; k < partitions.size(); k++) {
                    for( ;sizes.get(i)[k] - partitions.get(k).size()>0;) {

                        partitions.get(k).add(nodes.get(node).getNetworkID());
                        node++;
                    }
                }
                partitionScenarios.add(partitions);
                j++;
            }
        }
        return partitionScenarios;

    }

    // generateTwinPartitionPairs generates all useful ways to assign two twins to n partitions.
    public ArrayList<int []> generateTwinPartitionPairs(int n) {
        ArrayList<int []> pairs = new ArrayList<int []>();
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                pairs.add(new int [] {i, j});
            }
        }
        return pairs;
    }

    public  ArrayList<ArrayList<int []>> cartesianProduct(ArrayList<ArrayList<int []>> lists) {
        ArrayList<ArrayList<int []>> resultLists = new ArrayList<ArrayList<int []>>();
        if (lists.size() == 0) {
            resultLists.add(new ArrayList<int []>());
            return resultLists;
        } else {
            ArrayList<int []> firstList = lists.get(0);
            ArrayList<ArrayList<int []>> remainingLists = cartesianProduct(new ArrayList<ArrayList<int []>>(lists.subList(1, lists.size())));
            for (int [] condition : firstList) {
                for (ArrayList<int []> remainingList : remainingLists) {
                    ArrayList<int []> resultList = new ArrayList<int []>();
                    resultList.add(condition);
                    resultList.addAll(remainingList);
                    resultLists.add(resultList);
                }
            }
        }
        return resultLists;
    }
    // genPartitionSizes generates all possible combinations of partition sizes
    // for a system consisting of n nodes and up to k partitions, where at least
    // one partition must have a size greater than 'minSize'.
    public ArrayList<int []> genPartitionSizes(int n, int k, int minSize) {
        int [] s = new int [k];
        ArrayList<int []> sizes = new ArrayList<int []>();
        genPartitionSizesRecursive(0, n, minSize, s, sizes);
        return sizes;
    }
    
    public void genPartitionSizesRecursive(int i, int n, int minSize, int [] state, ArrayList<int []> sizes) {
        
        int [] s = new int [state.length];
        System.arraycopy(state, 0, s, 0, state.length);
    
        s[i] = n;
    
        // if s[i] <= s[i-1], we have found a new valid state
        if (i == 0 || s[i-1] >= n) {
            // must make a new copy of the state to avoid overwriting it
            int [] c = new int [s.length];
            System.arraycopy(s, 0, c, 0, s.length);
            sizes.add(c);
        }
    
        // find the next valid size for the current index
        int m = n - 1;
        if (i > 0) {
            m = Math.min(m, s[i-1]);
        }
    
        if (i+1 < s.length) {
            // decrement the current partition and recurse
            // for the first partition, we want to ensure that its size is at least 'minSize',
            // for the other partitions, we will allow it to go down to a size of 1.
            for (; (i == 0 && m >= minSize) || (i != 0 && m > 0); m--) {
                s[i] = m;
                genPartitionSizesRecursive(i+1, n-m, minSize, s, sizes);
            }
        }
        
    }

    // isValidTwinAssignment checks if the set of twinAssignments can be assigned to the partitions
    // with sizes specified by partitionSizes.
    public boolean isValidTwinAssignment(ArrayList<int []> twinAssignments, int [] partitionSizes) {
        int [] ps = new int [partitionSizes.length];
        System.arraycopy(partitionSizes, 0, ps, 0, partitionSizes.length);
        for (int i = 0; i < twinAssignments.size(); i++) {
            int first = twinAssignments.get(i)[0];
            if (first >= partitionSizes.length || ps[first] == 0) {
                return false;
            }
            ps[first]--;
            int second = twinAssignments.get(i)[1];
            if (second >= partitionSizes.length || ps[second] == 0) {
                return false;
            }
            ps[second]--;
        }
        return true;
    }

    public ArrayList<ArrayList<HashSet<Integer>>> getPartitionScenarios(){
        return partitionScenarios;
    }
}
