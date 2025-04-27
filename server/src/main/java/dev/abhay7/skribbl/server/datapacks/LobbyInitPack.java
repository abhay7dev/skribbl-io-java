package dev.abhay7.skribbl.server.datapacks;

import org.json.JSONException;
import org.json.JSONObject;

public class LobbyInitPack extends DataPackage {

    private String lobName;
    private boolean success;

    // is private is meaningless for !serverRequest packets
    public boolean isPrivate;

    // Server responds with whether creating the lobby was a success
    public LobbyInitPack(boolean success) {
        super(false);
        this.success = success;
        
    }

    // Client requests to create a lobby with this name
    public LobbyInitPack(String lobName, boolean isPrivate) {
        super(true);
        this.lobName = lobName;
        this.isPrivate = isPrivate;
    }

    public String getLobName() { return this.lobName; }
    public boolean isSuccess() { return this.success; }
    
    public static LobbyInitPack fromJSON(String json) throws JSONException {
        return fromJSON(new JSONObject(json));
    }
    public static LobbyInitPack fromJSON(JSONObject jo) throws JSONException {
        boolean isServerRequest = jo.getBoolean("isServerRequest");
    
        if (isServerRequest) {
            String lobName = jo.getString("lobName");
            boolean priv = jo.getBoolean("isPrivate");
            return new LobbyInitPack(lobName, priv);
        }
        boolean success = jo.getBoolean("success");
        return new LobbyInitPack(success);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jo = new JSONObject();
        jo.put("isServerRequest", this.isServerRequest());

        if (this.isServerRequest()) {
            jo.put("lobName", this.getLobName());
            jo.put("isPrivate", isPrivate);
        } else {
            jo.put("success", success);
        }

        return jo;
    }
}
