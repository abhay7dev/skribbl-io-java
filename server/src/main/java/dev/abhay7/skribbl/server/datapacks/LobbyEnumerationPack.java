package dev.abhay7.skribbl.server.datapacks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LobbyEnumerationPack extends DataPackage {
    public String lobby;
    public ArrayList<String> users;

    public LobbyEnumerationPack(String lobby) {
        super(true);
        this.lobby = lobby;
    }

    public LobbyEnumerationPack(String lobby, ArrayList<String> users) {
        // server response
        super(false);
        this.lobby = lobby;
        this.users = (ArrayList<String>) users.clone();
    }

    public static LobbyEnumerationPack fromJSON(String json) throws JSONException {
        return fromJSON(new JSONObject(json));
    }

    public static LobbyEnumerationPack fromJSON(JSONObject jo) throws JSONException {
        boolean isServerRequest = jo.getBoolean("isServerRequest");

        String lobby = jo.getString("lobby");
        if (!isServerRequest) {
            // must be server response
            ArrayList<String> mommu = new ArrayList<>();
            
            JSONArray arr = jo.getJSONArray("users");
            for (int i =0; i < arr.length(); ++i) {
                mommu.add(arr.getString(i));
            }
            return new LobbyEnumerationPack(lobby, mommu);
        }

        return new LobbyEnumerationPack(lobby);
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("isServerRequest", this.isServerRequest());
        jo.put("lobby", lobby);
        
        if (!isServerRequest()) {
            // server response
            JSONArray arr = new JSONArray();
            for (String user : this.users) {
                arr.put(user);
            }
            jo.put("users", arr);
        }

        return jo;
    }
}
