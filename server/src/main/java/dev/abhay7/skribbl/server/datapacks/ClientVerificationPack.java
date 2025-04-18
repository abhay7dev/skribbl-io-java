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

    private ClientVerificationPack(boolean isServerRequest, String vs, String username) {
        super(isServerRequest);
        this.verificationString = vs;
        this.username = username;
    }

    public ClientVerificationPack(String vs) {
        this(false, vs, "");
    }

    public ClientVerificationPack(String vs, String username) {
        this(true, vs, username);
    }

    public String getVerificationString() { return this.verificationString; }
    public String getUsername() { return this.username; }

    public static ClientVerificationPack fromJSON(String json) throws JSONException {
        JSONObject jo = new JSONObject(json);
        boolean isServerRequest = jo.getBoolean("isServerRequest");
        String vs = jo.getString("verificationString");
        if(isServerRequest) {
            String uname = jo.getString("username");
            return new ClientVerificationPack(vs, uname);
        }
        return new ClientVerificationPack(vs);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jo = new JSONObject();
        jo.put("isServerRequest", this.isServerRequest());
        jo.put("verificationString", this.getVerificationString());
        if(this.isServerRequest()) jo.put("username", this.getUsername());
        return jo;
    }
}
