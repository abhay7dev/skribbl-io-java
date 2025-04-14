package dev.abhay7.skribbl.server.datapacks;

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

}
