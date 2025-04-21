package dev.abhay7.skribbl.server;

public enum MessageType {
    CLIENT_VERIFICATION,
    LOBBY_JOIN,
    LOBBY_LIST,
    LOBBY_INIT,
    LOBBY_LEAVE,
    FETCH_WORDLIST,
    KEEP_ALIVE,
    GAME_DATA,
}