package controller;

import model.Client;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import model.AwayRoom;
import model.Room;

/**
 * Response Manager
 * Handles the responses given via the client thread
 */
public class ResponseManager {
    private JSONParser parser;
    private ServerManager serverManager;
    public ResponseManager(ServerManager sManager){
        this.parser = new JSONParser();
        this.serverManager = sManager;
    }

    public boolean processCommand(String command, Client client) {
        try {
            System.out.printf("Processing Command... ");
            JSONObject jsonObject = (JSONObject) parser.parse(command);
            String type = (String)jsonObject.get("type");
            System.out.printf("%s\n", type);
            switch (type){
                case "newidentity":
                    return processNewIdentity((String)jsonObject.get("identity"), client,
                            (String)jsonObject.get("auth"), (String)jsonObject.get("pass"));
                case "list":
                    processList(client);
                    return true;
                case "who":
                    processWho(client);
                    return true;
                case "createroom":
                    processCreateRoom((String)jsonObject.get("roomid"), client);
                    return true;
                case "join":
                    processJoin((String)jsonObject.get("roomid"), client);
                    return true;
                case "deleteroom":
                    processDeleteRoom((String)jsonObject.get("roomid"), client);
                    return true;
                case "message":
                    processMessage((String)jsonObject.get("content"), client);
                    return true;
                case "movejoin":
                    processMoveJoin((String)jsonObject.get("former"),
                                    (String)jsonObject.get("roomid"),
                                    (String)jsonObject.get("identity"), client);
                    return true;
                case "quit":
                    processQuit(client);
                    return false;
                default:
                    return true;
            }
        } catch (ParseException e) {
            return true;
        }
    }

    private void processMoveJoin(String former, String roomid, String identity, Client client) {
        System.out.printf("Move Join... ");
        System.out.printf("Setting Identity... ");
        client.setIdentitySilent(identity);
        //silently so it doesnt move the client into the mainhall
        System.out.printf("Sending Accept JSON... ");
        JSONObject accept = new JSONObject();
        accept.put("type", "serverchange");
        accept.put("approved", "true");
        accept.put("serverid", this.serverManager.getServerId());
        client.addToQueue(accept.toJSONString());
        System.out.printf("Moving To Room %s... ", roomid);
        serverManager.moveToRoom(roomid, former, client);
        System.out.printf("Done\n");
    }

    private boolean processNewIdentity(String identity, Client client, String auth, String pass) {
        System.out.printf("Creating New Identity... ");
        if (!serverManager.createNewIdentity(identity, auth, pass)) {
            //the servers rejected the identity
            client.failedIdentity();
            System.out.printf("Failed\n");
            return false;
        }
        System.out.printf("Setting Identity... ");
        client.setIdentity(identity);
        System.out.printf("Moving To Default Room... ");
        processDefaultRoomChange(client);
        System.out.printf("Done\n");
        return true;
    }

    private void processDefaultRoomChange(Client client) {
        serverManager.joinDefault(client, "");
    }

    private void processList(Client client) {
        System.out.printf("Fetching List... ");
        String[] roomids = serverManager.getAllRoomIds();
        System.out.printf("Assembling JSON... ");
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        for(String r : roomids){
            jsonArray.add(r);
        }
        jsonObject.put("type","roomlist");
        jsonObject.put("rooms",jsonArray);
        client.addToQueue(jsonObject.toJSONString());
        System.out.printf("Done\n");
    }

    private void processWho(Client client) {
        Room room = null;
        try {
            room = serverManager.getRoom(client);
            //get the room the client is in
        } catch (Exception e) {

        }
        JSONArray array = new JSONArray();
        String roomId = "";
        String owner = "NONE";
        if(room != null) {
            //if there is a room get all the clients within it
            Client[] clients = room.getAllClients();
            for (Client c : clients) {
                array.add(c.getIdentity());
            }
            roomId = room.getRoomId();
            owner = room.getOwner();
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type","roomcontents");
        jsonObject.put("roomid",roomId);
        jsonObject.put("identities",array);
        jsonObject.put("owner",owner);
        client.addToQueue(jsonObject.toJSONString());
    }

    private void processCreateRoom(String roomid, Client client) {
        JSONObject jsonObject = new JSONObject();
        boolean created = serverManager.createRoom(roomid, client);
        //was the room actually created
        String status = String.valueOf(created);
        jsonObject.put("type","createroom");
        jsonObject.put("roomid",roomid);
        jsonObject.put("approved",status);
        client.addToQueue(jsonObject.toJSONString());
        if(created){
            Room room = null;
            try {
                room = serverManager.getRoom(client);
                //get the clients room
                serverManager.moveToRoom(roomid, room.getRoomId(), client);
            } catch (Exception e) {

            }
        }
    }

    private void processJoin(String roomid, Client client) {
        System.out.printf("Joining %s... ",roomid);
        String former = "";
        try {
            former = serverManager.getRoom(client).getRoomId();
            //get the former room of the client
        } catch (Exception e) {}
        System.out.printf("From %s... ", former);
        AwayRoom away = serverManager.foriegnRoom(roomid);
        //does this room exist on another server
        if(away != null) { // the room exists on another server
            if(serverManager.canJoin(roomid)){
                System.out.printf("Moving to Foriegn Room... ");
                serverManager.moveToForiegnRoom(roomid, former, client, away);
            }else{
                //server dead
                serverManager.moveToRoom(former, former, client);
            }
        }
        else { // the room exists on this server
            System.out.printf("Moving to Room... ");
            serverManager.moveToRoom(roomid, former, client);
        }
        System.out.printf("Done\n");
    }

    private void processDeleteRoom(String roomid, Client client) {
        JSONObject jsonObject = new JSONObject();
        String status = String.valueOf(serverManager.deleteRoom(roomid, client));
        jsonObject.put("type","deleteroom");
        jsonObject.put("roomid",roomid);
        jsonObject.put("approved",status);
        client.addToQueue(jsonObject.toJSONString());
    }

    private void processMessage(String content, Client client) {
        serverManager.sendMessage(content,client); // just send a message
    }

    private void processQuit(Client client) {
        //deal with a quit request
        System.out.printf("Client Quitting... ");
        String former = "";
        try {
            former = serverManager.getRoom(client).getRoomId();
            // get the former room
        } catch (Exception e) {} // couldnt find former room
        serverManager.moveToRoom("", former, client);
        // move the client to no room hancing force qutting them
        System.out.printf("Done\n");
    }
}
