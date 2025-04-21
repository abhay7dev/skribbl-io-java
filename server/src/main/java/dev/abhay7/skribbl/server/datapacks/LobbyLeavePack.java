package dev.abhay7.skribbl.server.datapacks;

import org.json.JSONException;
import org.json.JSONObject;

public class LobbyLeavePack extends DataPackage {
    
    private boolean success;
    private String username;
    
    public LobbyLeavePack() {
        super(true);
        this.success = false;
    }

    public LobbyLeavePack(boolean success) {
        super(false);
        this.success = success;
    }

    public LobbyLeavePack(String username) {
        super(false);
        this.username = username;
    }

    public boolean isSuccess() { return this.success; }
    public String getUsername() { return this.username; }

    public static LobbyLeavePack fromJSON(String json) throws JSONException {
        JSONObject jo = new JSONObject(json);
        boolean isServerRequest = jo.getBoolean("isServerRequest");
    
        if (isServerRequest) {
            return new LobbyLeavePack();
        }

        try {
            String username = jo.getString("username");
            return new LobbyLeavePack(username);
        } catch(JSONException je) {}

        boolean success = jo.getBoolean("success");
        return new LobbyLeavePack(success);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jo = new JSONObject();
        jo.put("isRequest", this.isServerRequest());

        if (!this.isServerRequest()) {
            if(this.username == null) {
                jo.put("success", this.success);
            } else {
                jo.put("username", this.username);
            }
        }

        return jo;
    }
}
