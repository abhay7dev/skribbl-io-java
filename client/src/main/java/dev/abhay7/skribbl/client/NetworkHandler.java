package dev.abhay7.skribbl.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.JLabel;

import org.json.JSONException;
import org.json.JSONObject;

import dev.abhay7.skribbl.client.jameskwong.pwdsignal.PWDSignalSession;
import dev.abhay7.skribbl.client.jameskwong.pwdsignal.PWDSignalSessionState;
import dev.abhay7.skribbl.server.MessageType;
import dev.abhay7.skribbl.server.RawPacketHandler;
import dev.abhay7.skribbl.server.datapacks.ClientVerificationPack;
import dev.abhay7.skribbl.server.datapacks.DataPackage;
import dev.abhay7.skribbl.server.datapacks.GameDataEncryptedPack;
import dev.abhay7.skribbl.server.datapacks.GameDataPack;
import dev.abhay7.skribbl.server.datapacks.KeepAlivePack;
import dev.abhay7.skribbl.server.datapacks.LobbyEnumerationPack;
import dev.abhay7.skribbl.server.datapacks.LobbyInitPack;
import dev.abhay7.skribbl.server.datapacks.LobbyJoinPack;
import dev.abhay7.skribbl.server.datapacks.LobbyLeavePack;
import dev.abhay7.skribbl.server.datapacks.LobbyListPack;
import dev.abhay7.skribbl.server.datapacks.LobbyStartPack;
import dev.abhay7.skribbl.server.datapacks.PayloadPack;
import dev.abhay7.skribbl.server.datapacks.ServerLeavePack;
import dev.abhay7.skribbl.server.datapacks.WordsFetchPack;

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

    private volatile boolean isInitiatingJoin;

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
                        System.out.println(ioe);
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
                    System.out.println(e);
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

                if (pendingResponses.containsKey(type)) {
                    var f = pendingResponses.get(type);
                    f.complete(data);
                    System.out.println("FOUND A MATCHING FUTURE FOR " + type + " AND COMPLETED IT (" + f + ")");

                    pendingResponses.remove(type);
                } else {
                    System.out.println("PUTTING " + type + " INTO GENERAL QUEUE");
                    generalQueue.put(new ReceivedPacket(type, data));
                }

            } catch(Exception e) {
                System.out.println(e);
                // TODO: Implement error handling if we are unable to handle a packet
            }
        }

    }

    private void handlePacket(ReceivedPacket packet) throws JSONException {

        switch (packet.getType()) {
            case MessageType.PAYLOAD:{
                System.out.println("GEN QUEUE RECEIVED PAYLOAD PACKET");
                client.withPayload(PayloadPack.fromJSON(packet.getData()));
                break;
            }
            case MessageType.LOBBY_ENUMERATION:
                System.out.println("LOGIC ERROR; WHY DID WE RECEIVE LOBBY ENUMERATION CUZ IT SHOULD ONLY BE RECEIVED FROM A RESPONSE FUTURE");
                break;
            case MessageType.LOBBY_JOIN:
                client.updatePlayerList(LobbyJoinPack.fromJSON(packet.getData()).getPlayers());
                break;
            case MessageType.LOBBY_LEAVE:
                String username =  LobbyLeavePack.fromJSON(packet.getData()).getUsername();
                // make sure to remove them as well!!! or else the server is going to be receiving packets with destinations that go nowhere (OR EVEN WORSE, TO THE WRONG LOBBY!!)
                client.privateSessions.remove(username);
                client.getCurrentPlayersList().remove(username);
                client.updatePlayerList(client.getCurrentPlayersList());
                break;
            case MessageType.LOBBY_START:
                String firstPlayer = LobbyStartPack.fromJSON(packet.getData()).getFirstPlayer();
                if(client.getUsername().equals(firstPlayer)) {
                    client.getBoard().setDrawing(true);
                    client.startDrawing();
                } else {
                    client.getBoard().setDrawing(false);
                }
                client.updateMessages("GAME STARTED");
                break;
            
            case MessageType.GAME_DATA:
                
                {
                    if (client.inPrivate) {
                        System.out.println("uhhh... wtf???? we're in private mode but someone is trying to send public game data packages to us... HACKER???");
                        break;
                    }
                    handleRawGameDataPack(packet);
                }
                
                break;
            case MessageType.GAME_DATA_ENCRYPTED:
                {
                    if (!client.inPrivate) {
                        System.out.println("uhhh... wtf???? we're in public mode but someone is trying to send encrypted game data packages to us... HUH???");
                        break;
                    }

                    GameDataEncryptedPack enc = GameDataEncryptedPack.fromJSON(packet.getData());
                    if (!enc.dest.equals(client.getUsername())) {
                        System.out.println("The game data packet is addressed to " + enc.dest + ", which is not us (" + client.getUsername() + ")");
                        break;
                    }

                    if (!client.privateSessions.containsKey(enc.source)) {
                        System.out.println("Someone tried sending encyrpted data to us but we haven't handshaked with them yet");
                        break;
                    }

                    PWDSignalSession session = client.privateSessions.get(enc.source);

                    try {
                        byte[] data = session.decryptReceivePacket(enc.data);
                        // data is json bytes of GameDataPack

                        JSONObject gdp = new JSONObject(new String(data, StandardCharsets.UTF_8));
                        handleRawGameDataPack(new ReceivedPacket(MessageType.GAME_DATA, gdp));
                    }
                    catch (Exception ex) {
                        System.out.println("Failed to decrypt encrypted game packet from " + enc.source);
                    }  
                    
                    break;
                }
            default:
                break;
        }


    }

    private void handleRawGameDataPack(ReceivedPacket packet) {
        if(packet.getData().has("message") && !packet.getData().getString("message").isBlank()) {
            if(!packet.getData().has("type")) {
                if(client.getBoard().isDrawing()) {
                    String guess = packet.getData().getString("message").substring(packet.getData().getString("message").indexOf(":") + 2);
                    if(client.chosenWord.equalsIgnoreCase(guess)) {
                        try {
                            new Thread(() -> {
                                try {
                                    sendGameDataPack(new GameDataPack("SUCCESS_GUESS:" + guess + ":" + (int) ((30000 - (java.time.Instant.now().toEpochMilli() - this.client.startime)) / 100), "guess"));
                                } catch(Exception e) { System.err.println(e); }
                            }).start();
                        } catch(Exception e) { System.err.println(e); }
                    }
                }
                client.updateMessages(packet.getData().getString("message"));
            } else {
                if(packet.getData().getString("type").equalsIgnoreCase("word")) {
                    this.client.getBoard().addWordPhrase(packet.getData().getString("message"));
                } else if(packet.getData().getString("type").equalsIgnoreCase("guess")) {
                    if(packet.getData().getString("message").split(":")[0].equals("SUCCESS_GUESS") && packet.getData().getString("message").split(":")[1].equals(client.guessedWord)) {
                        this.client.updateMessages("GUESS IS CORRECT");
                        try {
                            this.client.currentPlayersMap.put(client.username, this.client.currentPlayersMap.get(client.username) + Integer.parseInt(packet.getData().getString("message").split(":")[2]));
                            this.client.usersPanel.removeAll();
                            for(String p: this.client.currentPlayersMap.keySet()) {
                                this.client.usersPanel.add(new JLabel((p.equals(this.client.getUsername()) ? p + " (You)" : p) + " - " + this.client.currentPlayersMap.get(p) + " Points"));
                            }
                            this.client.usersPanel.revalidate();
                            this.client.usersPanel.repaint();
                            try {
                                new Thread(() -> {
                                    try {
                                        sendGameDataPack(new GameDataPack(this.client.getUsername() + ":" + this.client.currentPlayersMap.get(this.client.username), "scoreupdate"));
                                    } catch(Exception e) { System.err.println(e); }
                                }).start();
                            } catch(Exception e) { System.err.println(e); }
                        } catch(Exception e) {
                            System.out.println(e);
                        }
                    }
                } else if(packet.getData().getString("type").equalsIgnoreCase("scoreupdate")) {
                    try {
                        String username = packet.getData().getString("message").split(":")[0];
                        int newScore = Integer.parseInt(packet.getData().getString("message").split(":")[1]);
                        this.client.currentPlayersMap.put(username, newScore);
                        this.client.usersPanel.removeAll();
                        for(String p: this.client.currentPlayersMap.keySet()) {
                            this.client.usersPanel.add(new JLabel((p.equals(this.client.getUsername()) ? p + " (You)" : p) + " - " + this.client.currentPlayersMap.get(p) + " Points"));
                        }
                        this.client.usersPanel.revalidate();
                        this.client.usersPanel.repaint();
                    } catch(Exception e) {
                        System.err.println(e);
                    }
                }
            }
        } else /* if(packet.getData().has("image")) */ {
            if(client.getBoard().getCanvas() != null) {
                GameDataPack gdp = GameDataPack.fromJSON(packet.getData());
                if(gdp != null) this.client.getBoard().getCanvas().setImage(gdp.getImage());
            }
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
        CompletableFuture<JSONObject> responseFuture = new CompletableFuture<>();
        this.pendingResponses.put(MessageType.LOBBY_LIST, responseFuture);
        this.setWriting(true);
        LobbyListPack llp = new LobbyListPack();
        sendDataPackage(llp, MessageType.LOBBY_LIST);
        this.setWriting(false);

        
        
        JSONObject json = responseFuture.get(5, TimeUnit.SECONDS);
        llp = LobbyListPack.fromJSON(json);
        
        return llp;
    }
    
    // Create lobby method
    protected synchronized LobbyInitPack createLobby(String lobbyName, boolean isPrivate) throws IOException, JSONException, InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<JSONObject> responseFuture = new CompletableFuture<>();
        this.pendingResponses.put(MessageType.LOBBY_INIT, responseFuture);
        this.setWriting(true);
        LobbyInitPack lip = new LobbyInitPack(lobbyName, isPrivate);
        sendDataPackage(lip, MessageType.LOBBY_INIT);
        this.setWriting(false);

        
        
        JSONObject json = responseFuture.get(5, TimeUnit.SECONDS);
        lip = LobbyInitPack.fromJSON(json);
        
        return lip;
    }

    // Join Public lobby
    protected synchronized LobbyJoinPack joinPublicLobby(String lobName) throws IOException, JSONException, InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<JSONObject> responseFuture = new CompletableFuture<>();
        this.pendingResponses.put(MessageType.LOBBY_JOIN, responseFuture);
        this.setWriting(true);
        LobbyJoinPack ljp = new LobbyJoinPack(lobName);
        sendDataPackage(ljp, MessageType.LOBBY_JOIN);
        this.setWriting(false);

        

        JSONObject json = responseFuture.get(5, TimeUnit.SECONDS);
        ljp = LobbyJoinPack.fromJSON(json);

        return ljp;
    }



    protected synchronized void sendPayload(String destination, byte[] data, String source) throws IOException, JSONException, InterruptedException, ExecutionException, TimeoutException {
        this.setWriting(true);
        PayloadPack pp = new PayloadPack(source, destination, data);
        sendDataPackage(pp, MessageType.PAYLOAD);
        this.setWriting(false);

        // CompletableFuture<JSONObject> responseFuture = new CompletableFuture<>();
        // this.pendingResponses.put(MessageType.LOBBY_JOIN, responseFuture);

        // JSONObject json = responseFuture.get(5, TimeUnit.SECONDS);
        // ljp = LobbyJoinPack.fromJSON(json);

        // return ljp;
    }

    protected synchronized Tuple<HashMap<String, PWDSignalSession>, LobbyJoinPack> joinPrivateLobby(String password, String lobName) throws IOException, JSONException, InterruptedException, ExecutionException, TimeoutException, Exception {
        CompletableFuture<JSONObject> responseFuture = new CompletableFuture<>();
        this.pendingResponses.put(MessageType.LOBBY_ENUMERATION, responseFuture);

        System.out.println("Writing enumeration pack...");
        this.setWriting(true);
        LobbyEnumerationPack lep = new LobbyEnumerationPack(lobName);
        sendDataPackage(lep, MessageType.LOBBY_ENUMERATION);
        this.setWriting(false);

        

        JSONObject json = responseFuture.get(5, TimeUnit.SECONDS);
        lep = LobbyEnumerationPack.fromJSON(json);


        if (!lep.lobby.equals(lobName)) {
            System.out.println("wtf?? resposnse lobby enuemeration didn't return the right lboby; logic error.");
        }
        System.out.println("Received enumeration pack...");

        // now we have all the users from the lobby
        // now we can initialize a connection to all of them

        HashMap<String, PWDSignalSession> mommu = new HashMap<>();

        for (String user : lep.users) {
            System.out.println("Establhisng handshake with player already in lobby: "  + user);
            PWDSignalSession session = new PWDSignalSession(password, true);

            byte[] p1 = session.createPayload1();
            PayloadPack pp = new PayloadPack(client.username, user, p1);
            responseFuture = new CompletableFuture<>();
            this.pendingResponses.put(MessageType.PAYLOAD, responseFuture);
            this.setWriting(true);
            sendDataPackage(pp, MessageType.PAYLOAD);
            this.setWriting(false);
            System.out.println("Sent payload 1... (future: " + responseFuture + ")");
            json = responseFuture.get(5, TimeUnit.SECONDS);
            pp = PayloadPack.fromJSON(json);
            System.out.println("Received payload 1...");
            session.acceptPayload1(pp.data, 0);
            System.out.println("Accepted payload 1...");

            if (!pp.source.equals(user) || !pp.dest.equals(client.username)) {
                System.out.println("MAJOR ERROR: the source/dest of payload 1 is totally wrong");
            }
            else {
                System.out.println("Successuflly negotiated payload 1 for destination of " + user);
            }
            
            p1 = session.createPayload2();
            pp = new PayloadPack(client.username, user, p1);
            responseFuture = new CompletableFuture<>();
            this.pendingResponses.put(MessageType.PAYLOAD, responseFuture);
            this.setWriting(true);
            sendDataPackage(pp, MessageType.PAYLOAD);
            this.setWriting(false);
            System.out.println("Sent payload 2...");

            json = responseFuture.get(5, TimeUnit.SECONDS);
            pp = PayloadPack.fromJSON(json);
            System.out.println("Received payload 2...");

            session.acceptPayload2(pp.data, 0);
            System.out.println("Accepted payload 2...");

            if (!pp.source.equals(user) || !pp.dest.equals(client.username)) {
                System.out.println("MAJOR ERROR: the source/dest of payload 2 is totally wrong");
            }
            else {
                System.out.println("Successuflly negotiated payload 2 for destination of " + user);
            }

            p1 = session.createPayload3();
            pp = new PayloadPack(client.username, user, p1);
            responseFuture = new CompletableFuture<>();
            this.pendingResponses.put(MessageType.PAYLOAD, responseFuture);
            this.setWriting(true);
            sendDataPackage(pp, MessageType.PAYLOAD);
            this.setWriting(false);
            System.out.println("Sent payload 3...");

            json = responseFuture.get(5, TimeUnit.SECONDS);
            pp = PayloadPack.fromJSON(json);
            System.out.println("Received payload 3...");
            session.acceptPayload3(pp.data, 0);
            System.out.println("Accepted payload 3...");

            if (!pp.source.equals(user) || !pp.dest.equals(client.username)) {
                System.out.println("MAJOR ERROR: the source/dest of payload 3 is totally wrong");
            }
            else {
                System.out.println("Successuflly negotiated payload 3 for destination of " + user);
            }

            mommu.put(user, session);
        }

        LobbyJoinPack ljp = joinPublicLobby(lobName);

        return new Tuple<>(mommu, ljp);
    }

    // Leave Lobby Method
    public synchronized LobbyLeavePack leaveLobby() throws IOException, JSONException, InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<JSONObject> responseFuture = new CompletableFuture<>();
        this.pendingResponses.put(MessageType.LOBBY_LEAVE, responseFuture);
        this.setWriting(true);
        LobbyLeavePack llp = new LobbyLeavePack();
        sendDataPackage(llp, MessageType.LOBBY_LEAVE);
        this.setWriting(false);
        
        

        JSONObject json = responseFuture.get(5, TimeUnit.SECONDS);
        llp = LobbyLeavePack.fromJSON(json);

        return llp;
    }

    // Leave Lobby Method
    public synchronized WordsFetchPack getWords() throws IOException, JSONException, InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<JSONObject> responseFuture = new CompletableFuture<>();
        this.pendingResponses.put(MessageType.FETCH_WORDLIST, responseFuture);
        this.setWriting(true);
        WordsFetchPack wfp = new WordsFetchPack();
        sendDataPackage(wfp, MessageType.FETCH_WORDLIST);
        this.setWriting(false);
        
        

        JSONObject json = responseFuture.get(5, TimeUnit.SECONDS);
        wfp = WordsFetchPack.fromJSON(json);

        return wfp;
    }

    protected synchronized void sendGameDataPack(GameDataPack gdp) throws IOException, JSONException, InterruptedException, ExecutionException, TimeoutException {
        this.setWriting(true);

        if (!client.inPrivate) {
            sendDataPackage(gdp, MessageType.GAME_DATA);
        }
        else {
            sendToClientsFriendsPriv(gdp);
        }

        this.setWriting(false);
    }

    // Send a message
    protected synchronized void sendMessage(String msg) throws IOException, JSONException, InterruptedException, ExecutionException, TimeoutException {
        this.setWriting(true);


        GameDataPack gdp = new GameDataPack(msg);

        if (!client.inPrivate) {
            sendDataPackage(gdp, MessageType.GAME_DATA);
        }
        else {
            sendToClientsFriendsPriv(gdp);
        }

        this.setWriting(false);
    }

    protected synchronized void sendWordUpdate(String msg) throws IOException, JSONException, InterruptedException, ExecutionException, TimeoutException {
        this.setWriting(true);


        GameDataPack gdp = new GameDataPack(msg, "word");

        if (!client.inPrivate) {
            sendDataPackage(gdp, MessageType.GAME_DATA);
            System.out.println("Sent word update");
        }
        else {
            sendToClientsFriendsPriv(gdp);
        }

        this.setWriting(false);
    }

    protected synchronized void sendBoard(BufferedImage image) throws IOException, JSONException, InterruptedException, ExecutionException, TimeoutException {
        this.setWriting(true);

        GameDataPack gdp = new GameDataPack(image);

        if (!client.inPrivate) {
            sendDataPackage(gdp, MessageType.GAME_DATA);
        }
        else {
            sendToClientsFriendsPriv(gdp);
        }

        this.setWriting(false);
    }

    private void sendToClientsFriendsPriv(GameDataPack gdp) {
        if (client.privateSessions == null) {
            System.out.println("Null sessions even though in private lobby??? WTF!");
        }

        if (client.privateSessions != null) {
            for (var entry : client.privateSessions.entrySet()) {
                if (entry.getValue().getState() != PWDSignalSessionState.PAYLOAD_3_VALIDATED) {
                    System.out.println("Handshake not fully finished with " + entry.getKey() + "; skipping him in list of receivers of encrypted game data");
                    continue;
                }

                try {
                    byte[] msg = gdp.toJSON().toString().getBytes(StandardCharsets.UTF_8);
                    GameDataEncryptedPack enc = new GameDataEncryptedPack(client.username, entry.getKey(), entry.getValue().encryptSendPacket(msg));
                    sendDataPackage(enc, MessageType.GAME_DATA_ENCRYPTED);
                }
                catch (Exception ex) {
                    System.out.println("Error creating encrypted game data packet to send: " + ex.getMessage());
                }
            }
        }
    }

    protected synchronized void startLobby(String firstPlayer) throws IOException, JSONException, InterruptedException, ExecutionException, TimeoutException {
        this.setWriting(true);
        LobbyStartPack lsp = new LobbyStartPack(firstPlayer);
        sendDataPackage(lsp, MessageType.LOBBY_START);
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
    public /*synchronized*/ boolean isVerified() { return this.isVerified; }
    public /*synchronized*/ void setVerified(boolean isVerified) { this.isVerified = isVerified; }
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
