package model;

/**
 * Server Configuration
 * Holds the information parsed from the config file
 */
public class ServerConfiguration {
    private String serverId;
    private String address;
    private int clientPort;
    private int serverPort;
    public ServerConfiguration(String serverId, String address,
                               String clientPort, String coordinationPort){
        this.serverId = serverId;
        this.address = address;
        this.clientPort = Integer.parseInt(clientPort);
        this.serverPort = Integer.parseInt(coordinationPort);
    }
    public String getServerId(){return serverId;}
    public String getAddress(){return address;}
    public int getClientPort(){return clientPort;}
    public int getServerPort(){return serverPort;}
}
