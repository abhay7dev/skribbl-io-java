package dev.abhay7.skribbl.server.datapacks;

import org.json.JSONException;
import org.json.JSONObject;

public class GameDataPack extends DataPackage {

    private String message = "";

    public GameDataPack(String message) {
        this(true, message);
    }

    public GameDataPack(boolean isServerRequest, String message) {
        super(isServerRequest);
        this.message = message;
    }

    public static GameDataPack fromJSON(String json) throws JSONException {
        return fromJSON(new JSONObject(json));
    }
    public static GameDataPack fromJSON(JSONObject jo) throws JSONException {
        boolean isServerRequest = jo.getBoolean("isServerRequest");

        if(jo.has("message")) {
            return new GameDataPack(isServerRequest, jo.getString("message"));
        }

        return null;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("isServerRequest", this.isServerRequest());

        if(this.message != null) {
            jo.put("message", this.getMessage());
        }

        return jo;
    }

    public String getMessage() { return this.message; }
    
}
