package bftsmart.twins;

public class NodeID {
    private int replicaID;
    private int networkID;
    public NodeID(int replicaID, int networkID) {
        this.replicaID = replicaID;
        this.networkID = networkID;
    }
    public void setReplicaID(int ReplicaID) {
        this.replicaID = ReplicaID;
    }
    public int getReplicaID() {
        return replicaID;
    }
    public void setNetworkID(int NetworkID) {
        this.networkID = NetworkID;
    }
    public int getNetworkID() {
        return networkID;
    }
    public String toString() {
        return "ReplicaID: " + replicaID + " NetworkID: " + networkID;
    }
}
