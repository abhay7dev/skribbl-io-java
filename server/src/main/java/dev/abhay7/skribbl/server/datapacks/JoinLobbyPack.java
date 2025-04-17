package dev.abhay7.skribbl.server.datapacks;

import org.json.JSONArray;
import org.json.JSONObject;

public class JoinLobbyPack extends DataPackage {

    private boolean success;
    private java.util.ArrayList<String> players;

    private String lobbyName;
    private String username;

    public JoinLobbyPack(boolean success) {
        super(false);
        this.success = success;
    }

    public JoinLobbyPack(boolean success, java.util.ArrayList<String> usernames) {
        super(false);
        this.success = success;
        this.players = usernames;
    }

    public JoinLobbyPack(String username, String lobbyName) {
        super(true);
        this.username = username;
        this.lobbyName = lobbyName;
    }

    public String getLobbyName() { return this.lobbyName; }
    public String getUsername() { return this.username; }
    public boolean isSuccess() { return this.success; }
    public java.util.ArrayList<String> getPlayers() { return this.players; }
    
    public static JoinLobbyPack fromJSON(String json) throws Exception {
        JSONObject jo = new JSONObject(json);
        boolean isRequest = jo.getBoolean("isRequest");
    
        if (isRequest) {
            String username = jo.getString("username");
            String lobbyName = jo.getString("lobbyName");
            return new JoinLobbyPack(username, lobbyName);
        } else {
            boolean success = jo.getBoolean("success");
            if (jo.has("players")) {
                org.json.JSONArray arr = jo.getJSONArray("players");
                java.util.ArrayList<String> players = new java.util.ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    players.add(arr.getString(i));
                }
                return new JoinLobbyPack(success, players);
            } else {
                return new JoinLobbyPack(success);
            }
        }
    }
    @Override
    public JSONObject toJSON() {
        JSONObject jo = new JSONObject();
        jo.put("isRequest", isRequest());

        if (isRequest()) {
            jo.put("username", username);
            jo.put("lobbyName", lobbyName);
        } else {
            jo.put("success", success);
            if (players != null) {
                JSONArray arr = new JSONArray();
                for (String p : players) {
                    arr.put(p);
                }
                jo.put("players", arr);
            }
        }

        return jo;
    }
}
