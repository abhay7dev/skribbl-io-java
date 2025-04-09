package dev.abhay7.skribbl.server.datapacks;

public class ClientVerificationPack extends DataPackage {

    private String verificationString;

    public ClientVerificationPack(String vs) {
        this.verificationString = vs;
    }

    public String getVerificationString() {
        return this.verificationString;
    }

    public String toString() { return this.getVerificationString(); }

}
