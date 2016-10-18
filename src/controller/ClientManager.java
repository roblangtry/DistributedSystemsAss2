package controller;

import model.Client;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Client Manager
 * Handles the accepting of new clients
 */
public class ClientManager {
    private ResponseManager responseManager;
    private int port;
    private ServerSocket serverSocket;
    private boolean running;
    public ClientManager(int port, ResponseManager rm) throws IOException {
        this.responseManager = rm;
        this.port = port;
        System.out.printf("Port %d used... ", port);
        this.serverSocket = new ServerSocket(port); // create a server
        serverSocket.setSoTimeout(5000); // prevent .accept() from blocking
    }
    public void start(){
        System.out.printf("Started\n");
        running = true;
        while(running){
            try {
                Socket socket = serverSocket.accept(); //accept a client
                System.out.printf("Accepting New Client... ");
                new Client(socket, responseManager); // create a client model
                System.out.printf("Done\n");
            } catch (IOException e) {} // do nothing
        }
    }

    public void shutdown() {
         // turn off the loop
        System.out.printf("Client Manager Shutdown... ");
    }
}
