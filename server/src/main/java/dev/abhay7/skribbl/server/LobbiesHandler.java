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

    public void addLobby(Lobby nLob) {
        lobbiesList.add(nLob);
    }

    
}
