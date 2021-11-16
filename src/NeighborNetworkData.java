public class NeighborNetworkData {
    final private String ipAddress;
    final private int routingPort;
    final private int forwardingPort;

    NeighborNetworkData(String ipAddress, int routingPort, int forwardingPort){
        this.ipAddress = ipAddress;
        this.routingPort = routingPort;
        this.forwardingPort = forwardingPort;
    }

    public int getRoutingPort() {
        return this.routingPort;
    }

    public int getForwardingPort() {
        return forwardingPort;
    }

    public String getIpAddress() {
        return ipAddress;
    }
}
