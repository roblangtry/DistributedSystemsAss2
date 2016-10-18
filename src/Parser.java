import model.ServerConfiguration;
import model.User;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * Parser
 * A class to parse the configuration file
 */
public class Parser {
    public String serverId;
    public String serverConfFilename;
    public ServerConfiguration[] otherServers;
    public ServerConfiguration configuration;
    public ArrayList<User> users;
    String password;
    public Parser(String[] args) throws Exception {
        users = new ArrayList<User>();
        String n = "",l = "";
        if(args.length != 4) throw new Exception();
        if(args[0].equals("-n")) n = args[1];
        else if(args[0].equals("-l")) l = args[1];
        else throw new Exception();
        if(args[2].equals("-n")) n = args[3];
        else if(args[2].equals("-l")) l = args[3];
        else throw new Exception();
        if(n.equals("") || l.equals("")) throw new Exception();
        this.serverId = n;
        this.serverConfFilename = l;
        this.getConfigurationFromFile();
    }
    private void getConfigurationFromFile() throws Exception{
        File file = new File(serverConfFilename);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        ServerConfiguration[] confs = new ServerConfiguration[0];
        boolean sysadmin = true;
        boolean user_info = false;
        while(reader.ready()){
            String line = reader.readLine();
            if(line.equals("")) user_info = true;
            else if(sysadmin) {
                this.password = line;
                sysadmin = false;
            }
            else if(user_info){
                String[] strings = line.split("\t", -1);
                if(strings.length != 2) throw new Exception();
                users.add(new User(strings[0],strings[1]));
            }else {
                String[] strings = line.split("\t", -1);
                if (strings.length != 4) throw new Exception();
                ServerConfiguration sConf = new ServerConfiguration(strings[0], strings[1], strings[2], strings[3]);
                confs = appendToList(confs, sConf);
            }
        }
        this.otherServers = new ServerConfiguration[0];
        this.configuration = null;
        for (ServerConfiguration conf: confs){
            if(conf.getServerId().equals(this.serverId)) this.configuration = conf;
            else this.otherServers = appendToList(this.otherServers, conf);
        }
        if(configuration == null) throw new Exception();
    }
    private static ServerConfiguration[] appendToList(ServerConfiguration[] list, ServerConfiguration element){
        ServerConfiguration[] newList = new ServerConfiguration[list.length + 1];
        int x = 0;
        for(ServerConfiguration elem : list){
            newList[x] = elem;
            x++;
        }
        newList[x] = element;
        return newList;
    }

    public ArrayList<User> getUsers() {
        return users;
    }

    public String getPassword(){
        return password;
    }
}
