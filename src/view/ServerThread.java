package view;

import org.json.simple.parser.ParseException;
import controller.ServerManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

/**
 * Server Thread
 * A thread for handling communication with a server from other servers
 */
public class ServerThread extends Thread {
    private SSLServerSocket serverSocket;
    private ServerManager serverManager;
    private boolean shuttingdown;
    public ServerThread(SSLServerSocket serverSocket,ServerManager serverManager){
        this.serverSocket = serverSocket;
        this.serverManager = serverManager;
        this.shuttingdown = false;
    }

    @Override
    public void run() {
        boolean running = true;
        while (running){
            try {
                SSLSocket socket = (SSLSocket) serverSocket.accept();
                socket.setSoTimeout(5000);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream());
                String command = reader.readLine();
                String outCommand = null;
                try {
                    outCommand = serverManager.processServerCommand(command);
                } catch (ParseException e) {
                    outCommand = "";
                }
                if(!outCommand.equals("")) {
                    writer.println(outCommand);
                    writer.flush();
                }
                writer.close();
                reader.close();
                socket.close();
            } catch (SocketTimeoutException ie) {
                if(shuttingdown)
                    running = false;
            } catch (IOException e) {
                running = false;
            }
        }
    }

    public void shutdown() {
        this.shuttingdown = true;
    }
}
