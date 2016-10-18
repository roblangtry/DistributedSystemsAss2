package model;

import model.Client;
import org.json.simple.JSONObject;

import java.util.ArrayList;

/**
 * Room
 * The model for a room
 */
public class Room {
    private String roomId;
    private String owner;
    private volatile ArrayList<Client> clients;
    public Room(String roomId, String owner){
        this.roomId = roomId;
        this.owner = owner;
        this.clients = new ArrayList<Client>();
    }
    public Client[] getAllClients(){
        Client[] list = new Client[clients.size()];
        int x = 0;
        for(Client c : clients){
            list[x] = c;
            x++;
        }
        return list;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getOwner() {
        return owner;
    }

    public void sendMessage(String identity, String content) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type","message");
        jsonObject.put("identity",identity);
        jsonObject.put("content",content);
        String message = jsonObject.toJSONString();
        int size = clients.size();
        for(int i = 0; i < size; i++){
            clients.get(i).addToQueue(message);
        }
    }

    public boolean hasClient(Client client) {
        for(Client c : clients){
            if(c.getIdentity().equals(client.getIdentity())) return true;
        }
        return false;
    }

    public void addClient(Client client, String former) {
        clients.add(client);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "roomchange");
        jsonObject.put("identity",client.getIdentity());
        jsonObject.put("former",former);
        jsonObject.put("roomid", this.getRoomId());
        this.sendToAllClients(jsonObject);
    }

    private void sendToAllClients(JSONObject jsonObject) {
        for(Client client : clients){
            client.addToQueue(jsonObject.toJSONString());
        }
    }

    public void removeClient(Client client) {
        int ind = -1;
        for(int i = 0; i < clients.size(); i++){
            if(clients.get(i).getIdentity().equals(client.getIdentity())) ind = i;
        }
        if(ind != -1)
            clients.remove(ind);
    }

    public void clientDisconnect(Client client) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "roomchange");
        jsonObject.put("identity",client.getIdentity());
        jsonObject.put("former",this.getRoomId());
        jsonObject.put("roomid", "");
        this.sendToAllClients(jsonObject);
    }

    public synchronized void shutdown() {
        for(Client client : clients){
            client.shutdown();
        }
    }
}
