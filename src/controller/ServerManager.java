package controller;

import model.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;

/**
 * Server Manager
 * Manages operations relating between servers and the client
 */
public class ServerManager {
    private String serverId;
    private String defaultRoomId;
    private ServerCommunicator serverCommunicator;
    private ArrayList<Room> myRooms;
    private ArrayList<AwayRoom> otherRooms;
    private ArrayList<StringLock> identityLocks;
    private ArrayList<StringLock> roomLocks;
    public ServerManager(String serverId, ServerCommunicator serverComms){
        this.serverId = serverId;
        this.defaultRoomId = "MainHall-" + serverId;
        this.myRooms = new ArrayList<Room>();
        this.otherRooms = new ArrayList<AwayRoom>();
        this.identityLocks = new ArrayList<StringLock>();
        this.roomLocks = new ArrayList<StringLock>();
        this.serverCommunicator = serverComms;
        myRooms.add(new Room(this.defaultRoomId, ""));
    }
    public Room getRoom(Client client) throws Exception {
        //get the room a client is in
        for ( Room room : myRooms){
            if(room.hasClient(client)) return room;
        }
        throw new Exception();
    }

    public String[] getAllRoomIds() {
        //get the room ids for all rooms both foriegn and on this server
        String[] rooms =
                new String[this.myRooms.size() + this.otherRooms.size()];
        int ind = 0;
        for(Room r : myRooms){
            rooms[ind] = r.getRoomId();
            ind++;
        }
        for(AwayRoom r : otherRooms){
            rooms[ind] = r.getRoomid();
            ind++;
        }
        return rooms;
    }

    public boolean createNewIdentity(String identity) {
        //create a new identity for a client by checking with other servers
        System.out.printf("Obtaining Locks... ");
        boolean allow = this.serverCommunicator.obtainIdentityLocks(identity,this.serverId);
        System.out.printf("Releasing Locks... ");
        this.serverCommunicator.releaseIdentityLocks(identity,this.serverId);
        return allow;
    }

    public void joinDefault(Client client, String former) {
        // join the default room
        Room room = this.getRoomById(this.defaultRoomId);
        room.addClient(client, former);
    }

    public boolean createRoom(String roomid, Client client) {
        //create a room by checking with other servers
        boolean allowed = serverCommunicator.obtainRoomLocks(this.serverId, roomid);
        serverCommunicator.releaseRoomLocks(allowed, this.serverId, roomid);
        if(allowed)
            myRooms.add(new Room(roomid, client.getIdentity()));
        return allowed;
    }

    public void moveToRoom(String roomid, String former, Client client) {
        //move a client to a new room
        Room oldRoom = this.getRoomById(former);
        boolean delete = false;
        System.out.printf("former - %s\n", former);
        if(!former.equals("") && oldRoom != null &&
                oldRoom.getOwner().equals(client.getIdentity()))
            delete = true;
        if(roomid.equals("")){
            System.out.printf("Disconnecting Client... ");
            if(!former.equals("")) {
                Room room = this.getRoomById(former);
                room.clientDisconnect(client);
            }
            this.removeClientFromRooms(client);
            if(delete)
                this.deleteRoom(former, client);
        }
        else {
            System.out.printf("Finding Room... ");
            this.removeClientFromRooms(client);
            Room room = this.getRoomById(roomid);
            if (room == null) {
                System.out.printf("Wrong Room Redirecting... ");
                room = this.getRoomById(former);
                delete = false;
            }
            room.addClient(client, former);
            if(delete)
                this.deleteRoom(former, client);
        }
    }

    private void removeClientFromRooms(Client client) {
        //remove a given client from all rooms on this server
        for(Room room : myRooms){
            room.removeClient(client);
        }
    }

    public String deleteRoom(String roomid, Client client) {
        //delete a room
        Room room = this.getRoomById(roomid);
        if(room.getOwner().equals(client.getIdentity())){
            Client[] clients = room.getAllClients();
            for(Client c : clients){
                this.moveToRoom(this.defaultRoomId, roomid, c);
            }
            int ind = -1;
            for(int i=0;i<myRooms.size();i++){
                if(myRooms.get(i).getRoomId().equals(roomid))
                    ind = i;
            }
            if(ind == -1)
                return "false";
            myRooms.remove(ind);
            serverCommunicator.informRoomDeletion(roomid,this.serverId);
            return "true";
        }
        else return "false";
    }

    private Room getRoomById(String roomid) {
        // get a room model by its id
        for(Room r : myRooms){
            if(r.getRoomId().equals(roomid)) return r;
        }
        return null;
    }

    public void sendMessage(String content, Client client) {
        //send a message
        Room room = null;
        try {
            room = this.getRoom(client);
            room.sendMessage(client.getIdentity(),content);
        } catch (Exception e) {}
    }

    public Room getDefaultRoom() {
        //get the default room
        Room room = null;
        for(Room r : myRooms){
            if(r.getRoomId().equals(defaultRoomId)) room = r;
        }
        return room;
    }

    public String processServerCommand(String command) throws ParseException {
        //process a command sent by another server
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject)parser.parse(command);
        String type = (String)jsonObject.get("type");
        switch (type){
            case "lockidentity":
                return this.lockIdentity(jsonObject);
            case "releaseidentity":
                return this.releaseIdentity(jsonObject);
            case "lockroomid":
                return this.lockRoom(jsonObject);
            case "releaseroomid":
                return this.releaseRoom(jsonObject);
            case "deleteroom":
                return this.deletedRoom(jsonObject);
            default:
                return "";
        }
    }

    private synchronized String lockIdentity(JSONObject jsonObject) {
        //lock an identity
        String identity = (String)jsonObject.get("identity");
        String owner = (String)jsonObject.get("serverid");
        JSONObject returnObject = new JSONObject();
        returnObject.put("type","lockidentity");
        returnObject.put("serverid", this.serverId);
        returnObject.put("identity", identity);
        boolean exists = this.checkIfIdentityExists(identity);
        boolean locks = this.checkIfIdentityLocked(identity);
        boolean obtained = !(locks || exists);
        if(obtained) identityLocks.add(new StringLock(identity,owner));
        returnObject.put("locked",String.valueOf(obtained));
        return returnObject.toJSONString();
    }

    private boolean checkIfIdentityLocked(String identity) {
        //check if an identity if locked
        for (StringLock i : identityLocks){
            if(i.getObject().equals(identity)) return true;
        }
        return false;
    }

    private boolean checkIfIdentityExists(String identity) {
        //check if an identity exists on this server
        for ( Room room : myRooms){
            Client[] clients = room.getAllClients();
            for(Client c : clients){
                if(c.getIdentity().equals(identity)) return true;
            }
        }
        return false;
    }

    private synchronized String releaseIdentity(JSONObject jsonObject) {
        //release the lock on an identity
        String identity = (String)jsonObject.get("identity");
        String owner = (String)jsonObject.get("serverid");
        int lock = -1;
        for(int i = 0; i < identityLocks.size();i++){
            if(identityLocks.get(i).getObject().equals(identity)) lock = i;
        }
        if(lock != -1){
            if(identityLocks.get(lock).getOwner().equals(owner))
                identityLocks.remove(lock);
        }
        return "";
    }

    private String lockRoom(JSONObject jsonObject) {
        // obtain a lock on a room
        String roomid = (String)jsonObject.get("roomid");
        String owner = (String)jsonObject.get("serverid");
        boolean exists = this.checkIfRoomExists(roomid);
        boolean locks = this.checkIfRoomLocked(roomid);
        boolean obtained = !(exists || locks);
        if(obtained) this.roomLocks.add(new StringLock(roomid, owner));
        JSONObject returnObject = new JSONObject();
        returnObject.put("type", "lockroomid");
        returnObject.put("serverid", this.serverId);
        returnObject.put("roomid", roomid);
        returnObject.put("locked", String.valueOf(obtained));
        return returnObject.toJSONString();
    }

    private boolean checkIfRoomExists(String roomid) {
        //check if a room exists on this server
        for(Room r : myRooms){
            if(r.getRoomId().equals(roomid)) return true;
        }
        return false;
    }

    private boolean checkIfRoomLocked(String roomid) {
        // check if a room lock exists on this server
        for(StringLock lock : this.roomLocks){
            if(lock.getObject().equals(roomid)) return true;
        }
        return false;
    }

    private String releaseRoom(JSONObject jsonObject) {
        //release a room lock
        String serverid = (String) jsonObject.get("serverid");
        String roomid = (String) jsonObject.get("roomid");
        boolean approved = ((String) jsonObject.get("approved")).equals("true");
        this.removeRoomLock(roomid,serverid);
        if(approved)
            this.addForiegnRoom(roomid,serverid);
        return "";
    }

    private void removeRoomLock(String roomid, String serverid) {
        //remove the room lock
        int lock = -1;
        for(int i = 0; i < this.roomLocks.size();i++){
            if(this.roomLocks.get(i).getObject().equals(roomid)) lock = i;
        }
        if(lock != -1){
            this.roomLocks.remove(lock);
        }
    }

    private void addForiegnRoom(String roomid, String serverid) {
        // add a room from another server
        this.otherRooms.add(new AwayRoom(roomid, serverid));
    }

    private String deletedRoom(JSONObject jsonObject) {
        // remove a room from another server
        String serverid = (String) jsonObject.get("serverid");
        String roomid = (String) jsonObject.get("roomid");
        int elem = -1;
        for(int i = 0; i < this.otherRooms.size(); i++)
            if (this.otherRooms.get(i).getRoomid().equals(roomid)) elem = i;
        if (elem != -1)
            this.otherRooms.remove(elem);
        return "";
    }

    public AwayRoom foriegnRoom(String roomid) {
        // get a room that exists on another server by id
        for(AwayRoom r : otherRooms){
            if(r.getRoomid().equals(roomid)) {
                return r;
            }
        }
        return null;
    }

    public void moveToForiegnRoom(String roomid, String former,
                                  Client client, AwayRoom awayRoom) {
        // move a client to another server
        JSONObject obj = new JSONObject();
        obj.put("type", "route");
        obj.put("roomid", roomid);
        System.out.printf("Getting Foriegn Details... ");
        ServerConfiguration config = serverCommunicator
                .getConfiguration(awayRoom.getServerid());
        obj.put("host", config.getAddress());
        obj.put("port", String.valueOf(config.getClientPort()));
        System.out.printf("Informing Client... ");
        client.addToQueue(obj.toJSONString());
    }

    public String getServerId() {
        return serverId;
    }

    public void shutdown() {
        //shutdown the server manager
        for (Room room : myRooms){
            room.shutdown();
        }
        System.out.printf("Server Manager Shutdown... ");
    }
}
