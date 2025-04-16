package dev.abhay7.skribbl.server.datapacks;

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
    
}
