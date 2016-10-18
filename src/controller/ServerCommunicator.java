package controller;

import model.ServerConfiguration;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import view.ServerThread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Server Communicator
 * Handles communication with other servers
 */
public class ServerCommunicator {
    ServerSocket serverSocket;
    ServerThread serverThread;
    ServerManager serverManager;
    ServerConfiguration[] otherServers;
    String serverId;
    public ServerCommunicator(String serverId, int serverPort,
                              ServerConfiguration[] otherServers)
                              throws IOException {
        this.serverSocket = new ServerSocket(serverPort);
        System.out.printf("Port %d used... ", serverPort);
        this.otherServers = otherServers;
        this.serverId = serverId;
    }
    public void start(ServerManager sManager){
        this.serverThread = new ServerThread(serverSocket, sManager);
        this.serverThread.start();
        this.serverManager = sManager;
        Timer timer = new Timer();
        TimerTask aliveTask = new TimerTask() {
            @Override
            public void run() {
                processCheckAlive();
            }
        };
        timer.schedule(aliveTask,10000,60000);

    }
    public void close(){
        try {
            serverThread.interrupt();
            serverSocket.close();
        } catch (IOException e) {}
    }

    public ArrayList<String> checkOtherServers(){
        ArrayList<String> aliveServers = new ArrayList<>();
        CommunicationNode node = null;
        for(ServerConfiguration config : otherServers){
            try {
                node = new CommunicationNode(config);
                JSONParser parser = new JSONParser();
                JSONObject obj = new JSONObject();
                obj.put("type","checkalive");
                obj.put("serverid", serverId);
                String command = obj.toJSONString();
                node.writeLine(command);
                String commandIn = node.readLine();
                JSONObject returnObj = (JSONObject)parser.parse(commandIn);
                aliveServers.add((String)returnObj.get("serverid"));
                node.close();
            } catch (IOException ioe) {
                try {
                    node.close();
                } catch(Exception e){

                }
            } catch (ParseException pe) {
                try {
                    node.close();
                } catch(Exception e){

                }
            }
        }
        return aliveServers;
    }

    private void processCheckAlive(){

        ArrayList<String> aliveServers = checkOtherServers();
        if(aliveServers.size()!=otherServers.length){
            //dead server exists
            ArrayList<String> deadServers = new ArrayList<>();
            for(ServerConfiguration config : otherServers){
                int count = 0;
                for(String id:aliveServers){
                    if(!id.equals(config.getServerId())){
                        count ++;
                    }
                }
                if(count==aliveServers.size()){
                    //add to dead server list
                    deadServers.add(config.getServerId());
                }
            }
            //handling
            for(String id :deadServers){
                System.out.println("dead server found: " + id);
                //remove chatrooms
                serverManager.deleteForeignRoom(id);
                //remove from otherServers

            }

        }

    }

    public boolean obtainIdentityLocks(String identity, String serverId) {
        // obtain identity locks on other servers
        boolean obtain = true;
        CommunicationNode node = null;
        for(ServerConfiguration config : otherServers){
            try {
                node = new CommunicationNode(config);
                JSONParser parser = new JSONParser();
                JSONObject obj = new JSONObject();
                obj.put("type","lockidentity");
                obj.put("serverid", serverId);
                obj.put("identity",identity);
                String command = obj.toJSONString();
                node.writeLine(command);
                String commandIn = node.readLine();
                JSONObject returnObj = (JSONObject)parser.parse(commandIn);
                boolean approved = ((String)returnObj
                        .get("locked")).equals("true");
                obtain = obtain && approved;
                node.close();
            } catch (IOException ioe) {
                try {
                    node.close();
                } catch(Exception e){

                }
            } catch (ParseException pe) {
                try {
                    node.close();
                } catch(Exception e){

                }
            }
        }
        return obtain;
    }

    public void releaseIdentityLocks(String identity, String serverId) {
        // release the identity locks held by other servers
        CommunicationNode node = null;
        for(ServerConfiguration config : otherServers){
            try {
                node = new CommunicationNode(config);
                JSONObject obj = new JSONObject();
                obj.put("type", "releaseidentity");
                obj.put("serverid",serverId);
                obj.put("identity", identity);
                node.writeLine(obj.toJSONString());
                node.close();
            } catch (IOException ioe) {
                try {
                    node.close();
                } catch(Exception e){

                }
            }
        }
    }

    public boolean obtainRoomLocks(String serverId, String roomid) {
        // obtain room locks from other servers
        boolean obtain = true;
        CommunicationNode node = null;
        for(ServerConfiguration config : otherServers){
            try {
                node = new CommunicationNode(config);
                JSONParser parser = new JSONParser();
                JSONObject obj = new JSONObject();
                obj.put("type","lockroomid");
                obj.put("serverid", serverId);
                obj.put("roomid",roomid);
                String command = obj.toJSONString();
                node.writeLine(command);
                String commandIn = node.readLine();
                JSONObject returnObj = (JSONObject)parser.parse(commandIn);
                boolean approved = ((String)returnObj
                                    .get("locked")).equals("true");
                obtain = obtain && approved;
                node.close();
            } catch (IOException ioe) {
                try {
                    node.close();
                } catch(Exception e){

                }
            } catch (ParseException pe) {
                try {
                    node.close();
                } catch(Exception e){

                }
            }
        }
        return obtain;
    }

    public void releaseRoomLocks(boolean allowed, String serverId,
                                 String roomid) {
        // release a held room lock on other servers
        CommunicationNode node = null;
        for(ServerConfiguration config : otherServers){
            try {
                node = new CommunicationNode(config);
                JSONObject obj = new JSONObject();
                obj.put("type", "releaseroomid");
                obj.put("serverid",serverId);
                obj.put("roomid", roomid);
                obj.put("approved", String.valueOf(allowed));
                node.writeLine(obj.toJSONString());
                node.close();
            } catch (IOException ioe) {
                try {
                    node.close();
                } catch(Exception e){

                }
            }
        }
    }

    public ServerConfiguration getConfiguration(String serverid) {
        //get a server configuration from its id
        for(ServerConfiguration config : otherServers){
            if(config.getServerId().equals(serverid))
                return config;
        }
        return null;
    }

    public void informRoomDeletion(String roomid, String serverId) {
        //tell the other servers a room was deleted
        CommunicationNode node = null;
        for(ServerConfiguration config : otherServers){
            try {
                node = new CommunicationNode(config);
                JSONObject obj = new JSONObject();
                obj.put("type", "deleteroom");
                obj.put("serverid",serverId);
                obj.put("roomid", roomid);
                node.writeLine(obj.toJSONString());
                node.close();
            } catch (IOException ioe) {
                try {
                    node.close();
                } catch(Exception e){

                }
            }
        }
    }

    public void shutdown() {
        serverThread.shutdown();
    }

    public class CommunicationNode{
        // just a quick wrapper for the server configuration class
        // for ease of communication
        private BufferedReader reader;
        private PrintWriter writer;
        private Socket socket;
        public CommunicationNode(ServerConfiguration configuration)
                throws IOException {
            this.socket = new Socket();
            SocketAddress address = new InetSocketAddress(
                    configuration.getAddress(), configuration.getServerPort());
            this.socket.connect(address,500);
            this.reader = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream());
        }
        public String readLine() throws IOException {
            return reader.readLine();
        }
        public void writeLine(String line){
            writer.println(line);
            writer.flush();
        }
        public void close(){
            try {
                this.socket.close();
            } catch (IOException e) {

            }
        }
    }

}
