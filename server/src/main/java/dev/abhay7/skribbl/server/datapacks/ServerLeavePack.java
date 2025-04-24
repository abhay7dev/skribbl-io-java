package dev.abhay7.skribbl.server.datapacks;

import org.json.JSONException;
import org.json.JSONObject;

public class ServerLeavePack extends DataPackage {

    public ServerLeavePack() {
        super(true);
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject jo = new JSONObject();
        return jo;
    }
    
}
