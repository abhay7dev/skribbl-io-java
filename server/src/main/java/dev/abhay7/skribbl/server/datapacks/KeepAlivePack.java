package dev.abhay7.skribbl.server.datapacks;

import org.json.JSONException;
import org.json.JSONObject;

public class KeepAlivePack extends DataPackage {

    public KeepAlivePack() {
        super(true);
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject jo = new JSONObject();
        return jo;
    }
    
}
