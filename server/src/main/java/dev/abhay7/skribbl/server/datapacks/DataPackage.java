package dev.abhay7.skribbl.server.datapacks;

public abstract class DataPackage implements java.io.Serializable {

    private boolean serverRequest;

    public DataPackage(boolean serverRequest) { this.serverRequest = serverRequest; }

    public boolean isServerRequest() { return this.serverRequest; }
    
    public abstract org.json.JSONObject toJSON() throws org.json.JSONException;
}
