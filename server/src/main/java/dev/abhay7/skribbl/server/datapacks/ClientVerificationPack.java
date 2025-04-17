package dev.abhay7.skribbl.server.datapacks;

import org.json.JSONObject;

public class ClientVerificationPack extends DataPackage {

    private String verificationString;

    public ClientVerificationPack(boolean isRequest, String vs) {
        super(isRequest);
        this.verificationString = vs;
    }

    public ClientVerificationPack(String vs) {
        this(false, vs);
    }

    

    public String getVerificationString() {
        return this.verificationString;
    }

    public String toString() { return this.getVerificationString(); }

    public static ClientVerificationPack fromJSON(String json) throws Exception {
        JSONObject jo = new JSONObject(json);
        boolean isBool = jo.getBoolean("isRequest");
        String vs = jo.getString("verificationString");
        return new ClientVerificationPack(isBool, vs);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jo = new JSONObject();
        jo.put("isRequest", isRequest());
        jo.put("verificationString", verificationString);
        return jo;
    }
}
