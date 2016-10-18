import controller.ClientManager;
import controller.ResponseManager;
import controller.ServerCommunicator;
import model.ServerConfiguration;
import controller.ServerManager;

/**
 * Main
 * The Class to be run to start the program
 */

public class Main {
    private ServerCommunicator serverCommunicator;
    private ServerManager serverManager;
    private ResponseManager responseManager;
    private ClientManager clientManager;
    private int thread_no;
    public void shutdown(){
        System.out.printf("Beginning Shutdown... ");
        serverCommunicator.shutdown();
        serverManager.shutdown();
        clientManager.shutdown();
        System.out.printf("Shutdown Complete\n");
    }
    public static void main(String[] args){
        Main main = new Main();
        main.go(args);
    }
    public void go(String[] args){
		System.setProperty("javax.net.ssl.keyStore","keystore");
		System.setProperty("javax.net.ssl.keyStorePassword","markwilson");
        Runtime.getRuntime().addShutdownHook(new ShutdownThread(this));
        try {
            System.out.printf("Creating Parser... ");
            Parser parseObject = new Parser(args);
            System.out.printf("Done\n");
            System.out.printf("Getting Configuration... ");
            ServerConfiguration configuration = parseObject.configuration;
            System.out.printf("Done\n");
            System.out.printf("Creating Server Communicator... ");
            serverCommunicator = new ServerCommunicator(
                    configuration.getServerId(), configuration.getServerPort(),
                    parseObject.otherServers);
            System.out.printf("Done\n");
            System.out.printf("Creating Server Manager... ");
            serverManager = new ServerManager(configuration.getServerId(),
                    serverCommunicator);
            System.out.printf("Done\n");
            System.out.printf("Starting Server Communicator... ");
            serverCommunicator.start(serverManager);
            System.out.printf("Done\n");
            System.out.printf("Creating Response Manager... ");
            responseManager = new ResponseManager(serverManager);
            System.out.printf("Done\n");
            System.out.printf("Creating Client Manager... ");
            clientManager = new ClientManager(configuration.getClientPort(), responseManager);
            System.out.printf("Done\n");
            System.out.printf("Starting Server... ");
            clientManager.start();
        } catch (Exception e) {
            System.out.printf("Failed\n");
            this.shutdown();
        }

    }
}
