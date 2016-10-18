package controller;

import model.Client;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

/**
 * Client Manager
 * Handles the accepting of new clients
 */
public class ClientManager {
    private ResponseManager responseManager;
    private int port;
    private SSLServerSocket serverSocket;
    private boolean running;
    SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory)
			SSLServerSocketFactory.getDefault();
    public ClientManager(int port, ResponseManager rm) throws IOException {
        this.responseManager = rm;
        this.port = port;
        System.out.printf("Port %d used... ", port);
        this.serverSocket = (SSLServerSocket) sslserversocketfactory.createServerSocket(port); // create a server
        serverSocket.setSoTimeout(5000); // prevent .accept() from blocking
    }
    public void start(){
		System.setProperty("javax.net.ssl.trustStore", "/Users/shmar/desktop/keystore.jks");

        System.out.printf("Started\n");
        running = true;
        while(running){
            try {
                SSLSocket socket = (SSLSocket) serverSocket.accept(); //accept a client
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
