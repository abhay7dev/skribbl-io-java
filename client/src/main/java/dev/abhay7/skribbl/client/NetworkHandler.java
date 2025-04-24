package dev.abhay7.skribbl.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.Socket;
import java.sql.Time;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONException;
import org.json.JSONObject;

import dev.abhay7.skribbl.server.MessageType;
import dev.abhay7.skribbl.server.RawPacketHandler;
import dev.abhay7.skribbl.server.datapacks.ClientVerificationPack;
import dev.abhay7.skribbl.server.datapacks.DataPackage;
import dev.abhay7.skribbl.server.datapacks.GameDataPack;
import dev.abhay7.skribbl.server.datapacks.KeepAlivePack;
import dev.abhay7.skribbl.server.datapacks.LobbyInitPack;
import dev.abhay7.skribbl.server.datapacks.LobbyJoinPack;
import dev.abhay7.skribbl.server.datapacks.LobbyLeavePack;
import dev.abhay7.skribbl.server.datapacks.LobbyListPack;
import dev.abhay7.skribbl.server.datapacks.ServerLeavePack;

public class NetworkHandler extends Thread {

    private static final String VERIFY_SUFFIX = "_VERIFIEDCONNECTION";

    private Client client;

    private Socket socket;
    private InputStream reader;
    private OutputStream writer;

    private volatile boolean isVerified;
    private volatile boolean isWriting;

    private final Map<MessageType, CompletableFuture<JSONObject>> pendingResponses;
    private final BlockingQueue<ReceivedPacket> generalQueue;

    public NetworkHandler(Client client, Socket s, InputStream reader, OutputStream writer) {
        super();

        this.client = client;
        this.socket = s;
        this.reader = reader;
        this.writer = writer;

        this.isVerified = false;

        generalQueue = new LinkedBlockingQueue<>();
        pendingResponses = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        new Thread(() -> {
            
            long lastPingRequest = java.time.Instant.now().toEpochMilli();

            while(isVerified() && client.isRunning() && !Thread.currentThread().isInterrupted()) {
                if(!isWriting()) {
                    setWriting(true);
                    try {
                        if(!socket.isConnected() || socket.isClosed()) {
                            throw new IOException("Socket connection has been closed");
                        }

                        long now = java.time.Instant.now().toEpochMilli();
                        if(now - lastPingRequest > 15000) {
                            sendDataPackage(new KeepAlivePack(), MessageType.KEEP_ALIVE);
                            lastPingRequest = now;
                        }
                    } catch(IOException ioe) {
                        // TODO: Implement error handling if we are unable to send a ping request
                    } finally {
                        setWriting(false);
                    }
                }
            }
        }).start();

        new Thread(() -> {
            while (isVerified() && client.isRunning() && !Thread.currentThread().isInterrupted()) {
                try {
                    ReceivedPacket packet = generalQueue.take(); // blocks if empty
                    handlePacket(packet);
                } catch (InterruptedException e) {
                    // TODO: Implement error handling if we are unable to read a packet
                }
            }
        }).start();

        while(this.isVerified() && client.isRunning() && !Thread.currentThread().isInterrupted()) {
            try {
                String rawPacket = RawPacketHandler.readRawPacket(this.reader);
                JSONObject obj = new JSONObject(rawPacket);
                MessageType type = MessageType.valueOf(obj.getString("type"));

                JSONObject data = obj.getJSONObject("data");

                System.out.println(data.toString());

                if (pendingResponses.containsKey(type)) {
                    pendingResponses.get(type).complete(data);
                    pendingResponses.remove(type);
                } else {
                    generalQueue.put(new ReceivedPacket(type, data));
                }

            } catch(Exception e) {
                // TODO: Implement error handling if we are unable to handle a packet
            }
        }

    }

    private void handlePacket(ReceivedPacket packet) throws JSONException {

        switch (packet.getType()) {
            case MessageType.LOBBY_JOIN:
                client.updatePlayerList(LobbyJoinPack.fromJSON(packet.getData()).getPlayers());
                break;
            case MessageType.LOBBY_LEAVE:
                String username =  LobbyLeavePack.fromJSON(packet.getData()).getUsername();
                client.getCurrentPlayersList().remove(username);
                client.updatePlayerList(client.getCurrentPlayersList());
            case MessageType.GAME_DATA:
                if(packet.getData().has("message")) {
                    client.updateMessages(packet.getData().getString("message"));
                } else {

                }
                break;
            default:
                break;
        }

    }

    // Verify method. This needs to happen to ensure proper connection to the server
    protected synchronized void verify(String username) throws IOException, JSONException, ProtocolException {
        JSONObject clientVerificationData = readJSONData(MessageType.CLIENT_VERIFICATION);
        ClientVerificationPack responseVerification = ClientVerificationPack.fromJSON(clientVerificationData);

        responseVerification = new ClientVerificationPack(responseVerification.getVerificationString() + NetworkHandler.VERIFY_SUFFIX, username);
        sendDataPackage(responseVerification, MessageType.CLIENT_VERIFICATION);

        String confirmString = readJSONDataToString(MessageType.CLIENT_VERIFICATION);
        ClientVerificationPack confirmPack = ClientVerificationPack.fromJSON(confirmString);
        
        if(confirmPack.isSuccess()) this.setVerified(true);
        else throw new ProtocolException("Failure");
    }

    // Get Lobby list method
    protected synchronized LobbyListPack retrieveLobbyList() throws IOException, JSONException, InterruptedException, ExecutionException, TimeoutException {
        this.setWriting(true);
        LobbyListPack llp = new LobbyListPack();
        sendDataPackage(llp, MessageType.LOBBY_LIST);
        this.setWriting(false);

        CompletableFuture<JSONObject> responseFuture = new CompletableFuture<>();
        this.pendingResponses.put(MessageType.LOBBY_LIST, responseFuture);
        
        JSONObject json = responseFuture.get(5, TimeUnit.SECONDS);
        llp = LobbyListPack.fromJSON(json);
        
        return llp;
    }
    
    // Create lobby method
    protected synchronized LobbyInitPack createPublicLobby(String lobbyName) throws IOException, JSONException, InterruptedException, ExecutionException, TimeoutException {
        this.setWriting(true);
        LobbyInitPack lip = new LobbyInitPack(lobbyName);
        sendDataPackage(lip, MessageType.LOBBY_INIT);
        this.setWriting(false);

        CompletableFuture<JSONObject> responseFuture = new CompletableFuture<>();
        this.pendingResponses.put(MessageType.LOBBY_INIT, responseFuture);
        
        JSONObject json = responseFuture.get(5, TimeUnit.SECONDS);
        lip = LobbyInitPack.fromJSON(json);
        
        return lip;
    }

    // Join Public lobby
    protected synchronized LobbyJoinPack joinPublicLobby(String lobName) throws IOException, JSONException, InterruptedException, ExecutionException, TimeoutException {
        this.setWriting(true);
        LobbyJoinPack ljp = new LobbyJoinPack(lobName);
        sendDataPackage(ljp, MessageType.LOBBY_JOIN);
        this.setWriting(false);

        CompletableFuture<JSONObject> responseFuture = new CompletableFuture<>();
        this.pendingResponses.put(MessageType.LOBBY_JOIN, responseFuture);

        JSONObject json = responseFuture.get(5, TimeUnit.SECONDS);
        ljp = LobbyJoinPack.fromJSON(json);

        return ljp;
    }

    // Leave Lobby Method
    public synchronized LobbyLeavePack leaveLobby() throws IOException, JSONException, InterruptedException, ExecutionException, TimeoutException {
        this.setWriting(true);
        LobbyLeavePack llp = new LobbyLeavePack();
        sendDataPackage(llp, MessageType.LOBBY_LEAVE);
        this.setWriting(false);
        
        CompletableFuture<JSONObject> responseFuture = new CompletableFuture<>();
        this.pendingResponses.put(MessageType.LOBBY_LEAVE, responseFuture);

        JSONObject json = responseFuture.get(5, TimeUnit.SECONDS);
        llp = LobbyLeavePack.fromJSON(json);

        return llp;
    }

    // Send a message
    public synchronized void sendMessage(String msg) throws IOException, JSONException, InterruptedException, ExecutionException, TimeoutException {
        this.setWriting(true);
        GameDataPack gdp = new GameDataPack(msg);
        sendDataPackage(gdp, MessageType.GAME_DATA);
        this.setWriting(false);
    }


    // Disconnect method that cleanly closes this thread
    public synchronized void disconnect() throws IOException {
        long disconnectRequestTime = java.time.Instant.now().toEpochMilli();
        while(this.isWriting() && java.time.Instant.now().toEpochMilli() < disconnectRequestTime + 5000) {}
        if(!this.isWriting()) sendDataPackage(new ServerLeavePack(), MessageType.SERVER_LEAVE);
        this.writer.close();
        this.reader.close();
        this.socket.close();
        this.setVerified(false);
    }

    // Network helper methods to handle the JSON data read/writes
    // Reads JSON Data through the network
    private String readJSONDataToString(MessageType mt) throws IOException, JSONException, ProtocolException {
        return readJSONData(mt).toString();
    }
    private JSONObject readJSONData(MessageType mt) throws IOException, JSONException, ProtocolException {
        String receivedJsonString = RawPacketHandler.readRawPacket(reader);
        
        JSONObject receivedJsonObject = new JSONObject(receivedJsonString);
        String type = receivedJsonObject.getString("type");
        
        if(MessageType.valueOf(type) != mt) { throw new ProtocolException("MessageType between requested package (" + (mt.toString()) + ") and response package (" + type + ") aren't the same"); }

        return receivedJsonObject.getJSONObject("data");
    }
    // Sends JSON Data through the network
    private void sendDataPackage(DataPackage dp, MessageType type) throws IOException {
        this.bufferDataPackage(dp, type);
        writer.flush();
    }
    private void bufferDataPackage(DataPackage dp, MessageType type) throws IOException {
        JSONObject dataEncapsulator = new JSONObject();
        dataEncapsulator.put("type", type.toString());
        dataEncapsulator.put("data", dp.toJSON());
        RawPacketHandler.bufferRawPacket(this.writer, dataEncapsulator.toString());
    }

    // Getters/Setters for a couple methods
    public synchronized boolean isVerified() { return this.isVerified; }
    public synchronized void setVerified(boolean isVerified) { this.isVerified = isVerified; }
    private synchronized boolean isWriting() { return this.isWriting; }
    private synchronized void setWriting(boolean writing) { this.isWriting = writing; }
    
    // Simple inner class declaration to hold the data for a ReceivedPaacket
    private class ReceivedPacket {
        private final MessageType type;
        private final JSONObject data;
    
        private ReceivedPacket(MessageType type, JSONObject data) { this.type = type; this.data = data; }
    
        private MessageType getType() { return type; }
        private JSONObject getData() { return data; }
    }
    

}
