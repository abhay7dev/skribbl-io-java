package dev.abhay7.skribbl.server;

public class Lobby {

    private String name;
    private boolean privateLob;

    public Lobby(String name) {
        this(name, false);
    }

    public Lobby(String name, boolean privateLob) {
        this.name = name;
        this.privateLob = privateLob;
    }

    public String getName() {
        return this.name;
    }

    public boolean isPrivate() {
        return this.privateLob;
    }
    
}
