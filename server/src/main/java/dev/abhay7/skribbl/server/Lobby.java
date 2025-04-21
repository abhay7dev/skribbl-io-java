package dev.abhay7.skribbl.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import dev.abhay7.skribbl.server.datapacks.DataPackage;
import dev.abhay7.skribbl.server.datapacks.LobbyInitPack;

public class Lobby {

    private String name;
    private Server.ClientCommsHandler host;

    private boolean started;

    private CopyOnWriteArrayList<Server.ClientCommsHandler> clients;

    public Lobby(String name, Server.ClientCommsHandler host) {
        this.name = name;
        this.host = host;
        this.started = false;
        clients = new CopyOnWriteArrayList<Server.ClientCommsHandler>();
        clients.add(this.host);
    }

    public Lobby(LobbyInitPack lip, Server.ClientCommsHandler host) {
        this(lip.getLobName(), host);
    }
    
    public String getName() { return this.name; }
    public String getHostname() { return this.host.getUsername(); }

    public boolean isHost(Server.ClientCommsHandler cch) {
        if(this.host.equals(cch)) return true;
        return false;
    }

    public void notifyAll(DataPackage dp, Server.ClientCommsHandler sender, MessageType type) throws IOException {
        for(Server.ClientCommsHandler cch: this.clients) {
            cch.sendDataPackage(dp, type);
        }
    }

    public void notifyAllExceptSender(DataPackage dp, Server.ClientCommsHandler sender, MessageType type) throws IOException {
        for(Server.ClientCommsHandler cch: this.clients) {
            if(!cch.equals(sender)) cch.sendDataPackage(dp, type);
        }
    }

    public void addClient(Server.ClientCommsHandler cch) {
        this.clients.add(cch);
    }

    public synchronized void removeClient(Server.ClientCommsHandler cch) {
        this.clients.remove(cch);
        if(this.host == cch) {
            this.host = null;
            if(this.getClients().size() > 0) this.host = this.clients.get(0);
        }
    }

    public void setHost(Server.ClientCommsHandler cch) {
        this.host = cch;
    }

    public ArrayList<String> getPlayerNames() {
        ArrayList<String> toRet = new ArrayList<>();
        for(Server.ClientCommsHandler cch: this.clients) {
            toRet.add(cch.getUsername());
        }
        return toRet;
    }

    public ArrayList<Server.ClientCommsHandler> getClients() {
        return new ArrayList<Server.ClientCommsHandler>(this.clients);
    }

    public boolean isStarted() { return this.started; }
    public void start() { this.started = true; }
    public void stop() { this.started = false; }
    
}