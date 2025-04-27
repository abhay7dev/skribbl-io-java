package dev.abhay7.skribbl.server.datapacks;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LobbyListPack extends DataPackage {
    
    private ArrayList<String[]> lobbiesData;

    // If client sends LobbyListPack, it will always be requesting the server
    public LobbyListPack() {
        super(true);
    }

    // Server responds with ArrayList of String[] arrays.
    // Each Each String[] contains data for each lobby, with String[0] being the server name, and String[1] being the number of people in the lobby. (Remember max is 10)
    // Server must use 
    private LobbyListPack(ArrayList<String[]> lobbiesData) {
        super(false);
        this.lobbiesData = lobbiesData;
    }

    // This is the "Server"'s response constructor that it will use to create the response
    public static LobbyListPack getFromLobbies(ArrayList<dev.abhay7.skribbl.server.Lobby> lobs) {
        ArrayList<String[]> toRet = new ArrayList<>();
        lobs.forEach((lob) -> {
            toRet.add(new String[]{lob.getName(), lob.getPlayerNames().size() + "", lob.getHostname(), lob.isPrivate + ""});
        });
        return new LobbyListPack(toRet);
    }

    public ArrayList<String[]> getLobbies() { return this.lobbiesData; }

    public static LobbyListPack fromJSON(String json) throws JSONException {
        return fromJSON(new JSONObject(json));
    }
    public static LobbyListPack fromJSON(JSONObject jo) throws JSONException {
        boolean isServerRequest = jo.getBoolean("isServerRequest");
    
        if (isServerRequest) return new LobbyListPack();

        JSONArray lobbiesData = jo.getJSONArray("lobbiesData");
        ArrayList<String[]> lobbies = new ArrayList<>();

        for (int i = 0; i < lobbiesData.length(); i++) {
            JSONArray lobbyData = lobbiesData.getJSONArray(i);

            String[] data = new String[lobbyData.length()];

            for (int j = 0; j < lobbyData.length(); j++) {
                data[j] = lobbyData.getString(j);
            }

            lobbies.add(data);
        }

        return new LobbyListPack(lobbies);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jo = new JSONObject();
        jo.put("isServerRequest", this.isServerRequest());

        if(this.isServerRequest()) return jo;

        JSONArray jsonLobbiesArray = new JSONArray();
        for (String[] entry: lobbiesData) {
            JSONArray aLobby = new JSONArray();
            aLobby.put(entry[0]);
            aLobby.put(entry[1]);
            aLobby.put(entry[2]);
            aLobby.put(entry[3]);
            jsonLobbiesArray.put(aLobby);
        }
        jo.put("lobbiesData", jsonLobbiesArray);

        return jo;
    }
}
