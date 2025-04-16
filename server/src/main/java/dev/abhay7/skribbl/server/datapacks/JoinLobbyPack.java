package dev.abhay7.skribbl.server.datapacks;

public class JoinLobbyPack extends DataPackage {

    private boolean success;
    private java.util.ArrayList<String> players;

    private String lobbyName;
    private String username;

    public JoinLobbyPack(boolean success) {
        super(false);
        this.success = success;
    }

    public JoinLobbyPack(boolean success, java.util.ArrayList<String> usernames) {
        super(false);
        this.success = success;
        this.players = usernames;
    }

    public JoinLobbyPack(String username, String lobbyName) {
        super(true);
        this.username = username;
        this.lobbyName = lobbyName;
    }

    public String getLobbyName() { return this.lobbyName; }
    public String getUsername() { return this.username; }
    public boolean isSuccess() { return this.success; }
    public java.util.ArrayList<String> getPlayers() { return this.players; }
    
}
