package dev.abhay7.skribbl.server;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

import dev.abhay7.skribbl.server.datapacks.DataPackage;
import dev.abhay7.skribbl.server.datapacks.LobbyInitPack;

public class Lobby {

    private String name;
    private boolean privateLob;
    private Server.ClientCommsHandler host;

    private CopyOnWriteArrayList<Server.ClientCommsHandler> clients;

    public Lobby(String name, Server.ClientCommsHandler host) {
        this(name, host, false);
    }

    public Lobby(String name, Server.ClientCommsHandler host, boolean privateLob) {
        this.name = name;
        this.host = host;
        this.privateLob = privateLob;
        clients = new CopyOnWriteArrayList<Server.ClientCommsHandler>();
        clients.add(this.host);
    }

    public Lobby(LobbyInitPack lip, Server.ClientCommsHandler host) {
        this(lip.getLobName(), host, lip.isPrivate());
    }
    
    public String getName() {
        return this.name;
    }

    public boolean isPrivate() {
        return this.privateLob;
    }

    public boolean isHost(Server.ClientCommsHandler cch) {
        if(this.host.equals(cch)) return true;
        return false;
    }

    // public void notifyHost(DataPackage dp) throws IOException {
    //     host.sendPackage(dp);
    // }

    public void notifyAllExceptSender(DataPackage dp, Server.ClientCommsHandler sender, MessageType type) throws IOException {
        for(Server.ClientCommsHandler cch: this.clients) {
            try {
                if(!cch.equals(sender)) cch.sendDP(dp, type);
            } catch(IllegalAccessError iae) {
                this.clients.remove(cch);
            }
        }
    }

    public void addClient(Server.ClientCommsHandler cch) {
        this.clients.add(cch);
    }

    public void setHost(Server.ClientCommsHandler cch) {
        this.host = cch;
    }

    public java.util.ArrayList<String> getPlayerNames() {
        java.util.ArrayList<String> toRet = new java.util.ArrayList<>();
        for(Server.ClientCommsHandler cch: this.clients) {
            toRet.add(cch.getUsername());
        }
        return toRet;
    }
    
}