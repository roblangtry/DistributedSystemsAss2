package model;

/**
 * Away Room
 * A class that represents a room on another server
 */
public class AwayRoom {
    String roomid;
    String serverid;
    public AwayRoom(String roomid, String serverid) {
        this.roomid = roomid;
        this.serverid = serverid;
    }
    public String getRoomid(){
        return roomid;
    }
    public String getServerid(){
        return serverid;
    }
}
