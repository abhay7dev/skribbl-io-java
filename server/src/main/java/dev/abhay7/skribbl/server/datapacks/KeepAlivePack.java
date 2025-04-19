package dev.abhay7.skribbl.server.datapacks;

import org.json.JSONException;
import org.json.JSONObject;

public class KeepAlivePack extends DataPackage {

    public KeepAlivePack(boolean serverRequest) {
        super(serverRequest);
    }

    public static KeepAlivePack fromJSON(String json) {
        JSONObject jo = new JSONObject(json);
        return new KeepAlivePack(jo.getBoolean("isServerRequest"));
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("isServerRequest", this.isServerRequest());
        return jo;
    }
    
}
