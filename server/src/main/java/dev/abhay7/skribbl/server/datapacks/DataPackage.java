package dev.abhay7.skribbl.server.datapacks;

import java.io.Serializable;

import org.json.JSONObject;

public abstract class DataPackage implements Serializable {

    private boolean request;

    public DataPackage(boolean isRequest) {
        this.request = isRequest;
    }

    public boolean isRequest() {
        return this.request;
    }

     public abstract JSONObject toJSON();
}
