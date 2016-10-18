package view;

import model.Client;
import org.json.simple.JSONObject;
import controller.ResponseManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Client Communication Thread
 * A thread that handles the communication for a single client
 */
public class ClientCommunicationThread extends Thread{
    private Socket socket;
    private Client client;
    private ResponseManager responseManager;
    private boolean running;
    private boolean shuttingdown;
    public ClientCommunicationThread(Socket socket, Client client, ResponseManager rm){
        this.socket = socket;
        this.client = client;
        this.responseManager = rm;
        this.shuttingdown = false;
    }

    @Override
    public void run() {
        boolean running = true;
        BufferedReader reader = null;
        PrintWriter writer = null;
        try {
            socket.setSoTimeout(500); // make reads non-blocking
            reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream());
        } catch (IOException e) {
            running = false;
        }
        while(running){
            try {
                this.checkClientQueue(writer);
                String command = reader.readLine();
                if (command != null) {
                    //System.out.printf("Received: %s\n", command);
                    //for debugging
                    running = responseManager.processCommand(command, client);
                } else {
                    running = false;
                    JSONObject obj = new JSONObject();
                    obj.put("type", "quit");
                    command = obj.toJSONString();
                    responseManager.processCommand(command, client);
                }
            } catch (SocketTimeoutException ie) {
                if (shuttingdown) {
                    running=false;
                    JSONObject obj = new JSONObject();
                    obj.put("type", "roomchange");
                    obj.put("identity", client.getIdentity());
                    obj.put("former", "");
                    obj.put("roomid", "");
                    String command = obj.toJSONString();
                    writer.println(command);
                    writer.flush();
                }
            } catch (IOException e) {
                running = false;
                JSONObject obj = new JSONObject();
                obj.put("type", "quit");
                String command = obj.toJSONString();
                responseManager.processCommand(command, client);
            }
        }
        if(!shuttingdown)
            this.checkClientQueue(writer); //clear queue
        try {
            writer.close();
            reader.close();
            socket.close();
        } catch (IOException e) {

        }
    }

    private void checkClientQueue(PrintWriter writer) {

        boolean running = true;
        while (running){
            try {
                String command = client.pollQueue();
                writer.println(command);
                writer.flush(); // always flush
                //System.out.printf("Sent: %s\n", command);
                // for debugging
            } catch (Exception e) {
                running = false;
            }
        }
    }

    public void shutdown() {
        shuttingdown = true;
    }
}
