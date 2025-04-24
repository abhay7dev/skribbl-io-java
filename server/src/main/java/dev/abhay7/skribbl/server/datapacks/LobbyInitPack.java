package dev.abhay7.skribbl.server.datapacks;

import org.json.JSONException;
import org.json.JSONObject;

public class LobbyInitPack extends DataPackage {

    private String lobName;
    private boolean success;

    // Server responds with whether creating the lobby was a success
    public LobbyInitPack(boolean success) {
        super(false);
        this.success = success;
    }

    // Client requests to create a lobby with this name
    public LobbyInitPack(String lobName) {
        super(true);
        this.lobName = lobName;
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
            return new LobbyInitPack(lobName);
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
        } else {
            jo.put("success", success);
        }

        return jo;
    }
}
