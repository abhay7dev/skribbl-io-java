package dev.abhay7.skribbl.server.datapacks;

import org.json.JSONException;
import org.json.JSONObject;

/* 
 * If (isServerRequest == false), that means that the server is sending the client something, and they only have to pass the verification string that the client must modify and return.
 * Else, the client is sending the CVP to the server after modifying it, which means isServerRequest needs to be true, and it has to pass the updated VS and a username
 */

public class ClientVerificationPack extends DataPackage {

    private String verificationString;
    private String username;
    private boolean success;

    private ClientVerificationPack(boolean isServerRequest, String vs, String username, boolean success) {
        super(isServerRequest);
        this.verificationString = vs;
        this.username = username;
        this.success = success;
    }
    
    public ClientVerificationPack(String vs) {
        this(false, vs, "", false);
    }
    public ClientVerificationPack(String vs, String username) {
        this(true, vs, username, false);
    }
    public ClientVerificationPack(boolean success) {
        this(false, null, null, success);
    }

    public String getVerificationString() { return this.verificationString; }
    public String getUsername() { return this.username; }
    public boolean isSuccess() { return this.success; }

    public static ClientVerificationPack fromJSON(String json) throws JSONException {
        return fromJSON(new JSONObject(json));
    }
    public static ClientVerificationPack fromJSON(JSONObject jo) throws JSONException {
        boolean isServerRequest = jo.getBoolean("isServerRequest");
        boolean isSuccess = jo.getBoolean("isSuccess");

        if(isServerRequest && jo.has("verificationString")) {
            String uname = jo.getString("username");
            return new ClientVerificationPack(jo.getString("verificationString"), uname);
        } else {
            if(isSuccess == false && jo.has("verificationString")) {
                return new ClientVerificationPack(jo.getString("verificationString"));
            }
        }
        return new ClientVerificationPack(isSuccess);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jo = new JSONObject();
        jo.put("isServerRequest", this.isServerRequest());
        jo.put("isSuccess", this.isSuccess());
        jo.put("verificationString", this.getVerificationString());
        if(this.isServerRequest()) jo.put("username", this.getUsername());
        return jo;
    }
}
