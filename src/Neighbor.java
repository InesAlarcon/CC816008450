public class Neighbor {
    private final String name;
    private final int cost;
    private final NeighborNetworkData networkData;

    Neighbor(String name, int cost, String ip, int routingPort, int forwardingPort){
        this.name = new String(name);
        this.cost = cost;
        this.networkData = new NeighborNetworkData(ip, routingPort, forwardingPort);
    }

    // Getters
    public String getName() {
        return this.name;
    }

    public int getCost() {
        return this.cost;
    }

    public NeighborNetworkData getNetworkData() {
        return networkData;
    }
}
