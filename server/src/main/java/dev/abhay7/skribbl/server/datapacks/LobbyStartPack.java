package dev.abhay7.skribbl.server.datapacks;

import org.json.JSONException;
import org.json.JSONObject;

public class LobbyStartPack extends DataPackage {
    
    private String firstPlayer;

    public LobbyStartPack(String firstPlayer) {
        this(true, firstPlayer);   
    }

    public LobbyStartPack(boolean isRequest, String firstPlayer) {
        super(isRequest);
        this.firstPlayer = firstPlayer;        
    }

    public String getFirstPlayer() { return this.firstPlayer; }

    public static LobbyStartPack fromJSON(String json) throws JSONException {
        return fromJSON(new JSONObject(json));
    }
    public static LobbyStartPack fromJSON(JSONObject jo) throws JSONException{
        System.out.println(jo.toString());
        return new LobbyStartPack(jo.getBoolean("isServerRequest"), jo.getString("firstPlayer"));
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jo = new JSONObject();
        jo.put("isServerRequest", this.isServerRequest());
        jo.put("firstPlayer", this.getFirstPlayer());
        System.out.println(jo.toString());
        return jo;
    }

}
