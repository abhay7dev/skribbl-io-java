package dev.abhay7.skribbl.server;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class LobbiesHandler {

    private CopyOnWriteArrayList<Lobby> lobbiesList;

    public LobbiesHandler() {
        lobbiesList = new CopyOnWriteArrayList<Lobby>();
    }

    public ArrayList<Lobby> getLobbyArrayList() {
        return new ArrayList<Lobby>(lobbiesList);
    }

    public void addLobby(Lobby nLob) throws IllegalArgumentException {
        if(isLobbyNameAvailable(nLob)) {
            lobbiesList.add(nLob);
        } else {
            throw new IllegalArgumentException("Lobby name: " + nLob.getName() + " already taken.");
        }
    }

    public void removeLobby(Lobby oLob) {
        if(oLob != null) lobbiesList.remove(oLob);
    }

    public boolean isLobbyNameAvailable(Lobby lob) {
        for(Lobby l: lobbiesList) {
            if(l.getName().equals(lob.getName())) return false;
        }
        return true;
    } 

    public Lobby getLobbyByName(String n) {
        for(Lobby l: lobbiesList) {
            if(l.getName().equals(n)) return l;
        }
        return null;
    }

    
}
