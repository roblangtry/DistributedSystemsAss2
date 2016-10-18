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

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Server Communicator
 * Handles communication with other servers
 */
public class ServerCommunicator {
	SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory)
			SSLServerSocketFactory.getDefault();
    SSLServerSocket serverSocket;
    ServerThread serverThread;
    ServerManager serverManager;
    ServerConfiguration[] otherServers;
    String serverId;
    int port;
    ArrayList<String> aliveServers;
    public ServerCommunicator(String serverId, int serverPort,
                              ServerConfiguration[] otherServers)
                              throws IOException {
        this.serverSocket = (SSLServerSocket) sslserversocketfactory.createServerSocket(serverPort);
        System.out.printf("Port %d used... ", serverPort);
        this.otherServers = otherServers;
        this.serverId = serverId;
        this.port = serverPort;
        aliveServers = new ArrayList<>();
        for (ServerConfiguration server : otherServers){
            aliveServers.add(server.getServerId());
        }
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
        ArrayList<String> oldServers = aliveServers;
        aliveServers = new ArrayList<>();
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
        for(String server : oldServers){
            if(!aliveServers.contains(server))
                System.out.printf("Server '%s' is down\n", server);
        }
        for(String server : aliveServers){
            if(!oldServers.contains(server))
                System.out.printf("Server '%s' is up\n", server);
        }
        return aliveServers;
    }

    private void processCheckAlive(){
        aliveServers = checkOtherServers();
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
                //remove chatrooms
                serverManager.deleteForeignRoom(id);
                //remove from otherServers
            }
        }
    }
    public boolean obtainIdentityLocks(String identity, String serverId,
                                       String auth, String pass) {
        // obtain identity locks on all servers
        boolean obtain = true;
        boolean authorised = false;
        CommunicationNode node = null;
        ServerConfiguration[] allServers =
                new ServerConfiguration[otherServers.length+1];
        for(int i=0;i<otherServers.length;i++)
            allServers[i]=otherServers[i];
        allServers[allServers.length-1] = new ServerConfiguration(this.serverId,
                "127.0.0.1",String.valueOf(this.port),String.valueOf(this.port));
        for(ServerConfiguration config : allServers){
            try {
                node = new CommunicationNode(config);
                JSONParser parser = new JSONParser();
                JSONObject obj = new JSONObject();
                obj.put("type","lockidentity");
                obj.put("serverid", serverId);
                obj.put("identity",identity);
                obj.put("auth", auth);
                obj.put("pass", pass);
                String command = obj.toJSONString();
                node.writeLine(command);
                String commandIn = node.readLine();
                JSONObject returnObj = (JSONObject)parser.parse(commandIn);
                boolean approved = ((String)returnObj
                        .get("locked")).equals("true");
                boolean authed = ((String)returnObj
                        .get("authorised")).equals("true");
                obtain = obtain && approved;
                authorised = authorised || authed;
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
        if(!obtain) System.out.printf("Couldn't Lock... ");
        if(!authorised) System.out.printf("Couldn't Authorise... ");
        return obtain && authorised;
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
    public void addNewServer(ServerConfiguration newServerConfig){
        ServerConfiguration[] configs = new ServerConfiguration[otherServers.length+1];
        int i = 0;
        for(ServerConfiguration server : otherServers){
            configs[i] = server;
            i++;
        }
        configs[i] = newServerConfig;
        otherServers = configs;
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
        private SSLSocket socket;
        public CommunicationNode(ServerConfiguration configuration)
                throws IOException {
            SSLSocketFactory sslsocketfactory =
                    (SSLSocketFactory) SSLSocketFactory.getDefault();
            this.socket = (SSLSocket) sslsocketfactory.createSocket();
            this.socket.connect(new InetSocketAddress(configuration.getAddress(), configuration.getServerPort()),500);
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
