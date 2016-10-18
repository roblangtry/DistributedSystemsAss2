package model;

import view.ClientCommunicationThread;
import org.json.simple.JSONObject;
import controller.ResponseManager;

import java.net.Socket;
import java.util.LinkedList;

/**
 * Client
 * A model representing a connection
 */
public class Client {
    LinkedList<String> queue;
    private String identity;
    private ClientCommunicationThread thread;

    public Client(Socket socket, ResponseManager rm){
        queue = new LinkedList<String>();
        thread = new ClientCommunicationThread(socket, this, rm);
        thread.start();
    }
    public synchronized void addToQueue(String action){
        queue.add(action);
    }
    public synchronized String pollQueue() throws Exception {
        if(queue.size()==0)throw new Exception();
        return queue.pop();
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "newidentity");
        jsonObject.put("approved", "true");
        String action = jsonObject.toJSONString();
        this.addToQueue(action);
    }
    public void setIdentitySilent(String identity){
        this.identity = identity;
    }

    public void failedIdentity() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "newidentity");
        jsonObject.put("approved", "false");
        String action = jsonObject.toJSONString();
        this.addToQueue(action);
    }

    public synchronized void shutdown() {
        thread.shutdown();
        while (thread.isAlive())
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {

            }
    }
}
