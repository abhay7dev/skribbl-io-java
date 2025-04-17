package dev.abhay7.skribbl.server.datapacks;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import dev.abhay7.skribbl.server.Lobby;

public class LobbyListPack extends DataPackage {
    
    private ArrayList<String[]> lobbiesData;

    public LobbyListPack() {
        super(true);
    }

    public LobbyListPack(ArrayList<String[]> lobbiesData, boolean isRequest) {
        super(isRequest);
        this.lobbiesData = lobbiesData;
    }

    public LobbyListPack(ArrayList<Lobby> lobs) {
        super(false);
        lobbiesData = new ArrayList<String[]>();
        lobs.forEach((l) -> {
            lobbiesData.add(new String[]{l.getName(), l.isPrivate() + ""}); 
        });
    }

    public ArrayList<String[]> getLobbies() {
        return this.lobbiesData;
    }

    public static LobbyListPack fromJSON(String json) throws Exception {
        org.json.JSONObject jo = new org.json.JSONObject(json);
        boolean isRequest = jo.getBoolean("isRequest");
    
        if (isRequest) {
            return new LobbyListPack();
        }

        
        if (jo.has("lobbiesData")) {
            org.json.JSONArray arr = jo.getJSONArray("lobbiesData");
            java.util.ArrayList<String[]> lobbies = new java.util.ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONArray inner = arr.getJSONArray(i);
                String[] data = new String[inner.length()];
                for (int j = 0; j < inner.length(); j++) {
                    data[j] = inner.getString(j);
                }
                lobbies.add(data);
            }
            return new LobbyListPack(lobbies, false);
        }
        else {
            return null;
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jo = new JSONObject();
        jo.put("isRequest", isRequest());

        if (!isRequest() && lobbiesData != null) {
            JSONArray outer = new JSONArray();
            for (String[] entry : lobbiesData) {
                JSONArray inner = new JSONArray();
                inner.put(entry[0]);
                inner.put(entry[1]);
                outer.put(inner);
            }
            jo.put("lobbiesData", outer);
        }

        return jo;
    }
}
