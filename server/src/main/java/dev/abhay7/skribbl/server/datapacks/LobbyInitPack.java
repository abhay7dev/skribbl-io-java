package dev.abhay7.skribbl.server.datapacks;

import org.json.JSONObject;

public class LobbyInitPack extends DataPackage {

    private String lobName;
    private boolean isPrivateB;
    private String username;
    private boolean success;

    public LobbyInitPack(boolean success) {
        super(false);
        this.success = success;
    }

    public LobbyInitPack(String lobName, String username, boolean isPrivateB) {
        super(true);
        this.lobName = lobName;
        this.username = username;
        this.isPrivateB = isPrivateB;
    }

    public String getLobName() { return this.lobName; }
    public String getUsername() { return this.username; }
    public boolean isPrivate() { return this.isPrivateB; }
    public boolean isSuccess() { return this.success; }
    
    public static LobbyInitPack fromJSON(String json) throws Exception {
        org.json.JSONObject jo = new org.json.JSONObject(json);
        boolean isRequest = jo.getBoolean("isRequest");
    
        if (isRequest) {
            String lobName = jo.getString("lobName");
            String username = jo.getString("username");
            boolean isPrivateB = jo.getBoolean("isPrivateB");
            return new LobbyInitPack(lobName, username, isPrivateB);
        } else {
            boolean success = jo.getBoolean("success");
            return new LobbyInitPack(success);
        }
    }
    @Override
    public JSONObject toJSON() {
        JSONObject jo = new JSONObject();
        jo.put("isRequest", isRequest());

        if (isRequest()) {
            jo.put("lobName", lobName);
            jo.put("username", username);
            jo.put("isPrivateB", isPrivateB);
            // if you later add a password field, do:
            // if (isPrivateB) jo.put("password", password);
        } else {
            jo.put("success", success);
        }

        return jo;
    }
}
