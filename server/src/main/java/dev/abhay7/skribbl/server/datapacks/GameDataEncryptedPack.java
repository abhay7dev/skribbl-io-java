package dev.abhay7.skribbl.server.datapacks;

import java.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

public class GameDataEncryptedPack extends DataPackage {
    public String source;
    public String dest;
    public byte[] data;
    
    public GameDataEncryptedPack(String source, String dest, byte[] data) {
        super(false);
        this.source = source;
        this.dest = dest;
        this.data = data;
    }

    public static GameDataEncryptedPack fromJSON(String json) throws JSONException {
        return fromJSON(new JSONObject(json));
    }
    public static GameDataEncryptedPack fromJSON(JSONObject jo) throws JSONException {
        return new GameDataEncryptedPack(jo.getString("source"), jo.getString("dest"), Base64.getDecoder().decode(jo.getString("data")));
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jo = new JSONObject();
        jo.put("source", source);
        jo.put("dest", dest);
        jo.put("data", Base64.getEncoder().encodeToString(data));

        return jo;
    }
}
