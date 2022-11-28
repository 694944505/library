package bftsmart.twins;

public class Settings {
    private int numNodes;
    private int numTwins;
    private int partitions;
    private int views;
    private int ticks;
    private boolean shuffle;
    private long seed;

    public Settings(int numNodes, int numTwins, int partitions, int views) {
        this.numNodes = numNodes;
        this.numTwins = numTwins;
        this.partitions = partitions;
        this.views = views;
        this.ticks = 0;
        this.shuffle = false;
        this.seed = 0;
    }

    public void setNumNodes(int numNodes) {
        this.numNodes = numNodes;
    }
    public int getNumNodes() {
        return numNodes;
    }
    public void setNumTwins(int numTwins) {
        this.numTwins = numTwins;
    }
    public int getNumTwins() {
        return numTwins;
    }
    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }
    public int getPartitions() {
        return partitions;
    }
    public void setViews(int views) {
        this.views = views;
    }
    public int getViews() {
        return views;
    }
    public void setTicks(int ticks) {
        this.ticks = ticks;
    }
    public int getTicks() {
        return ticks;
    }
    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }
    public boolean getShuffle() {
        return shuffle;
    }
    public void setSeed(long seed) {
        this.seed = seed;
    }
    public long getSeed() {
        return seed;
    }

}
