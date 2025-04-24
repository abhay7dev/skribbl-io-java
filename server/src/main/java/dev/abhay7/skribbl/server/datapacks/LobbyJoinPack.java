package dev.abhay7.skribbl.server.datapacks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class LobbyJoinPack extends DataPackage {

    private boolean success;
    private ArrayList<String> players;
    private boolean started;

    private String lobbyName;

    // Server response with success being false if no data is sent
    public LobbyJoinPack() {
        super(false);
        this.success = false;
    }
    // Server response with success being true if usernames are sent
    public LobbyJoinPack(java.util.ArrayList<String> usernames, boolean started) {
        super(false);
        this.success = true;
        this.players = usernames;
        this.started = started;
    }

    // This is what client sends to join a specific lobby
    public LobbyJoinPack(String lobbyName) {
        super(true);
        this.lobbyName = lobbyName;
    }

    public String getLobbyName() { return this.lobbyName; }
    public boolean isSuccess() { return this.success; }
    public boolean isStarted() { return this.started; }
    public ArrayList<String> getPlayers() { return this.players; }
    
    public static LobbyJoinPack fromJSON(String json) throws JSONException {
        return fromJSON(new JSONObject(json));
    }
    public static LobbyJoinPack fromJSON(JSONObject jo) throws JSONException {
        boolean isServerRequest = jo.getBoolean("isServerRequest");
    
        // This is where server will most likely end up
        if (isServerRequest) {
            String lobbyName = jo.getString("lobbyName");
            return new LobbyJoinPack(lobbyName);
        }

        // Client will recieve this from server
        boolean success = jo.getBoolean("success");
        if (success) {
            JSONArray arr = jo.getJSONArray("players");
            java.util.ArrayList<String> players = new java.util.ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                players.add(arr.getString(i));
            }
            // Succesful Join
            return new LobbyJoinPack(players, jo.getBoolean("started"));
        }
        
        // Failed join
        return new LobbyJoinPack();
    }


    @Override
    public JSONObject toJSON() {
        JSONObject jo = new JSONObject();
        jo.put("isServerRequest", this.isServerRequest());

        // Client requesting to join server
        if (this.isServerRequest()) {
            jo.put("lobbyName", this.getLobbyName());
        } else {
            // Server responding to client whether it is a succesful join or not
            jo.put("success", this.isSuccess());
            if (this.isSuccess()) {
                JSONArray arr = new JSONArray();
                for (String p: this.getPlayers()) {
                    arr.put(p);
                }
                jo.put("players", arr);
                jo.put("started", this.isStarted());
            }
        }

        return jo;
    }
}
