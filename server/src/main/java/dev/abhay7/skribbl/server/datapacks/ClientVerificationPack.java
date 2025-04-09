package dev.abhay7.skribbl.server.datapacks;

import dev.abhay7.skribbl.server.DataPackage;

public class ClientVerificationPack extends DataPackage {

    private String verificationString;

    public ClientVerificationPack(String vs) {
        this.verificationString = vs;
    }

    public String getVerificationString() {
        return this.verificationString;
    }

}
