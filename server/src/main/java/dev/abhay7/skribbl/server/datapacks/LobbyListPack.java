package dev.abhay7.skribbl.server.datapacks;

import java.util.ArrayList;

import dev.abhay7.skribbl.server.Lobby;

public class LobbyListPack extends DataPackage {
    
    private ArrayList<String[]> lobbiesData;

    public LobbyListPack() {
        super(true);
    }

    public LobbyListPack(ArrayList<Lobby> lobs) {
        super(false);
        lobbiesData = new ArrayList<String[]>();
        lobs.forEach((l) -> {
            lobbiesData.add(new String[]{l.getName(), l.isPrivate() + ""}); 
        });
    }

    public ArrayList<String[]> getLobbies() {
        return this.lobbiesData;
    }

}
