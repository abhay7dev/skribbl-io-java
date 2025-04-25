package dev.abhay7.skribbl.server;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONObject;

import dev.abhay7.skribbl.server.datapacks.*;

public class Server {

    // max of 16MB packets in one data send
    protected static final int MAX_RAW_PACKET_LENGTH = 1024 * 1024 * 16;

    private ServerSocket serverSocket;
    private boolean isRunning;

    private ArrayList<String> wordList;

    private CopyOnWriteArrayList<String> usernames;
    private CopyOnWriteArrayList<Thread> connectedClientList;
    private CopyOnWriteArrayList<ClientCommsHandler> connectedClientListRunnables;

    private LobbiesHandler lobbies;

    public Server(int PORT, String wordListSource) {
        try {
            this.usernames = new CopyOnWriteArrayList<String>();
            this.connectedClientList = new CopyOnWriteArrayList<Thread>();
            this.connectedClientListRunnables = new CopyOnWriteArrayList<ClientCommsHandler>();
            this.lobbies = new LobbiesHandler();

            wordList = new ArrayList<String>();
            initializeWordList(wordListSource);

            serverSocket = new ServerSocket(PORT);
            isRunning = true;
        
            System.out.println("Skribbl Server running on port " + PORT + "!");
        
        } catch (Exception e) {
            System.err.println("Fatal Error. Failed to start Skribbl Server on port " + PORT + "\n" + e);
        }

        runServer();
    }

    private void initializeWordList(String wordListSource) {
        try {
            
            URL url = new URI(wordListSource).toURL();
            
            if (url.getProtocol().equals("http") || url.getProtocol().equals("https")) {
            
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String line = null;

                while ((line = reader.readLine()) != null) {
                    wordList.add(line.split(",")[0]);
                }
            
                reader.close();
            } else { throw new Exception("Ignored: Not a valid http(s) url"); }

        } catch (Exception ignored) {
            
            try {
            
                Path path = Paths.get(wordListSource);

                if (path.isAbsolute() || !wordListSource.trim().isEmpty()) {
                    BufferedReader reader = Files.newBufferedReader(path);
                    String line = null;

                    while ((line = reader.readLine()) != null) {
                        wordList.add(line.split(",")[0]);
                    }
                
                    reader.close();
                } else {
                    throw new Exception("Not a valid file url");
                }
            
            } catch(Exception e) {
                System.out.println("Fatal Error. Couldn't initialize word list from: '" + wordListSource + "'");
                System.exit(1);
            }
        }
        if(wordList.size() < 10) {
            System.out.println("Initialized word list has a size less than 10. Choose a source with more words.");
            System.exit(1);
        }
    }

    public void runServer() {
        while (isRunning) {
            Socket clientSocket = null;
            OutputStream writer = null;
            InputStream reader = null;

            try {
                clientSocket = serverSocket.accept();
                
                reader = clientSocket.getInputStream();
                writer = clientSocket.getOutputStream();
            } catch (Exception e) {
                System.err.println("Failed to accept client socket and establish IO streams.");
            }

            if (clientSocket != null && writer != null && reader != null) {
                System.out.println("New user connected with following Inet Address: " + clientSocket.getInetAddress());

                ClientCommsHandler cch = new ClientCommsHandler(clientSocket, writer, reader);
                Thread userThread = new Thread(cch);
                cch.setThisRunnableWrapper(userThread);
                connectedClientListRunnables.add(cch);
                connectedClientList.add(userThread);
                userThread.start();
            }

        }
    }

    protected class ClientCommsHandler implements Runnable {

        private Socket clientSocket;
        private OutputStream writer;
        private InputStream reader;
        private Thread thisRunnableWrapper;

        private boolean verified = false;
        private String socketId = "UNINITIALIZED";
        
        private String username = null;

        private Lobby currentLobby = null;

        private static int waittime = 30000;
        private static String verifyString = "_VERIFIEDCONNECTION";

        private ClientCommsHandler(Socket cs, OutputStream writer, InputStream reader) {
            this.clientSocket = cs;
            this.writer = writer;
            this.reader = reader;
            this.socketId = java.time.Instant.now().toEpochMilli() + "_" + Math.random();
        }

        @Override
        public void run() {
            String receivedJsonString;
            DataPackage receivedClientData;

            try {
                ClientVerificationPack cvp = new ClientVerificationPack(Math.random() + "");
                sendDataPackage(cvp, MessageType.CLIENT_VERIFICATION); // Wraps cvp as [length in 4 bytes ][{ type: CLIENT_VERIFICATION, data: { blah blah blah}}]
                this.clientSocket.setSoTimeout(waittime / 3);

                receivedJsonString = RawPacketHandler.readRawPacket(reader);

                JSONObject receivedJsonObject = new JSONObject(receivedJsonString);
                String type = receivedJsonObject.getString("type");
                
                if(MessageType.valueOf(type) != MessageType.CLIENT_VERIFICATION) {
                    throw new Exception("Invalid MessageType in response");
                } else {
                
                    String innerDataString = receivedJsonObject.getJSONObject("data").toString();
                    ClientVerificationPack responseVerification = ClientVerificationPack.fromJSON(innerDataString);

                    if (responseVerification.getVerificationString() == null || responseVerification.getUsername() == null || responseVerification.getUsername().equals("") || !responseVerification.getVerificationString().equals(cvp.getVerificationString() + verifyString)) { 
                        throw new Exception("Invalid verification response");
                    } else if(usernames.contains(responseVerification.getUsername())) {
                        throw new Exception("Username already taken");
                    } else {
                        System.out.println("Client successfully verified");
                        this.username = responseVerification.getUsername();
                        usernames.add(this.username);                 
                        this.clientSocket.setSoTimeout(waittime);
                        this.verified = true;
                        sendDataPackage(new ClientVerificationPack(true), MessageType.CLIENT_VERIFICATION);
                    }
                }

            } catch (Exception e) {
                System.out.println("A client failed to respond to verification in a valid manner, will be removed: " + e);
            }

            try {
                while (this.isVerified()) {
                    receivedJsonString = RawPacketHandler.readRawPacket(reader);

                    // First get the type
                    JSONObject obj = new JSONObject(receivedJsonString);
                    String type = obj.getString("type");

                    // Then get actual data from inside object
                    JSONObject innerData = obj.getJSONObject("data");
                    String innerDataString = innerData.toString();
                    
                    // Based on the MessageType, we do different things
                    MessageType mt = MessageType.valueOf(type);

                    switch (mt) {
                        case LOBBY_INIT:
                            receivedClientData = LobbyInitPack.fromJSON(innerDataString);
                            break;
                        case LOBBY_JOIN:
                            receivedClientData = LobbyJoinPack.fromJSON(innerDataString);
                            break;
                        case LOBBY_LIST:
                            receivedClientData = LobbyListPack.fromJSON(innerDataString);
                            break;
                        case LOBBY_LEAVE:
                            receivedClientData = LobbyLeavePack.fromJSON(innerDataString);
                            break;
                        case GAME_DATA:
                            receivedClientData = GameDataPack.fromJSON(innerDataString);
                            break;
                        case FETCH_WORDLIST:
                            receivedClientData = WordsFetchPack.fromJSON(innerDataString);
                            break;
                        case KEEP_ALIVE:
                            receivedClientData = new KeepAlivePack();
                            break;
                        case SERVER_LEAVE:
                            this.verified = false;
                            receivedClientData = new ServerLeavePack();
                            break;
                        default:
                            throw new Exception("Unsupported message type");
                    }
                
                    handleDataPackage(receivedClientData);
                
                }
                
            } catch (EOFException e) {
                System.out.println("Client socket disconnected");
            } catch (SocketTimeoutException e) {
                System.out.println("Client socket timed out");
            } catch (Exception e) {
                System.out.println("Error in recieving/sending client data. Likely a malformed/malicious/disconnected client: " + e);
            } finally {
                this.disconnectAndTerminateUser();
            }

        }

        private void disconnectAndTerminateUser() {
            try {
                this.writer.close();
                this.reader.close();
                this.clientSocket.close();
                if (this.thisRunnableWrapper != null) {
                    connectedClientList.remove(this.thisRunnableWrapper);
                    connectedClientListRunnables.remove(this);
                    // thisRunnableWrapper.join();
                }
                if(this.isInLobby()) {
                    this.leaveLobby();
                }
                if(this.username != null) {
                    usernames.remove(this.username);
                }
                System.out.println("Disconnected " + this.username);
            } catch (Exception e) {
                System.out.println("Failed to disconnect and terminate a user: " + this.socketId + " - " + this.username + ": " + e);
            }
        }

        private void handleDataPackage(DataPackage dataPackage) {
            
            System.out.println("Received Data Package: " + dataPackage);

            if (this.verified && dataPackage != null && dataPackage.isServerRequest()) {
            
                if (dataPackage instanceof LobbyListPack) {
                    LobbyListPack toSend = LobbyListPack.getFromLobbies(lobbies.getLobbyArrayList());
                    try {
                        this.sendDataPackage(toSend, MessageType.LOBBY_LIST);
                        System.out.println("Sent LobbyListPack with lobbies list to " + this.username);
                    } catch (Exception e) {
                        System.out.println("Failed to send LobbyListPack to client: " + e);
                    }

                } else if (dataPackage instanceof LobbyInitPack) {

                    LobbyInitPack p = (LobbyInitPack) dataPackage;
                    Lobby lob = null;
                    
                    try {
                        lob = new Lobby(p, this);
                        lobbies.addLobby(lob);

                        sendDataPackage(new LobbyInitPack(true), MessageType.LOBBY_INIT);
                        this.currentLobby = lob;
                    } catch (IllegalArgumentException iae) {
                        System.out.println(iae);
                        try {
                            sendDataPackage(new LobbyInitPack(false), MessageType.LOBBY_INIT);
                            // After receiving this package, client should initialize its own game. Because
                            // games run on the "host", not the server itself, which only facilitates
                            // connection and communication.
                        } catch (Exception e) {
                            System.out.println("Failed to send failure to client: " + e);
                            lob.removeClient(this);
                            lobbies.removeLobby(lob);
                            this.currentLobby = null;
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to notify of success creating lobby. Removing lobby...");
                        lob.removeClient(this);
                        lobbies.removeLobby(lob);
                        this.currentLobby = null;
                        lob = null;
                    }

                } else if (dataPackage instanceof LobbyJoinPack) {

                    String lobName = ((LobbyJoinPack) dataPackage).getLobbyName();

                    Lobby lob = lobbies.getLobbyByName(lobName);
                    if (this.username != null && lob != null) {
                        try {
                            lob.addClient(this);
                            sendDataPackage(new LobbyJoinPack(lob.getPlayerNames(), lob.isStarted()), MessageType.LOBBY_JOIN);
                            this.currentLobby = lob;
                            
                            System.out.println("Notifying all except sender (" + this.username + ")");
                            lob.notifyAllExceptSender(new LobbyJoinPack(lob.getPlayerNames(), lob.isStarted()), this, MessageType.LOBBY_JOIN);
                        } catch (Exception e) {
                            System.out.println("Failed to notify about joining lobby");
                            lob.removeClient(this);
                            this.currentLobby = null;
                            try {
                                sendDataPackage(new LobbyJoinPack(), MessageType.LOBBY_JOIN);
                            } catch (Exception ex) {
                                System.out.println("Failed to send failure of joining lobby: " + ex);
                            }
                        }
                    } else {
                        try {
                            sendDataPackage(new LobbyJoinPack(), MessageType.LOBBY_JOIN);
                        } catch (Exception e) {
                            System.out.println("Failed to send failure of joining lobby: " + e);
                        }
                    }
                } else if(dataPackage instanceof GameDataPack) {
                    if(this.isInLobby()) {
                        try {
                            GameDataPack gdp = ((GameDataPack) dataPackage);
                            if(!gdp.getMessage().isBlank()) {
                                this.getCurrentLobby().notifyAllExceptSender(new GameDataPack(false, this.getUsername() + ": " + gdp.getMessage()), this, MessageType.GAME_DATA);
                            } else if(gdp.getImage() != null) {
                                this.getCurrentLobby().notifyAllExceptSender(new GameDataPack(false, gdp.getImage()), this, MessageType.GAME_DATA);
                            }
                        } catch(Exception e) {
                            System.out.println("Failed to send GameDataPack to cliens: " + e);
                        }
                    }
                } else if(dataPackage instanceof WordsFetchPack) {
                    if(this.isInLobby()) {
                        WordsFetchPack toSend = new WordsFetchPack(wordList);
                        try {
                            this.sendDataPackage(toSend, MessageType.FETCH_WORDLIST);
                            System.out.println("Sent WordsFetchPack with words list to " + this.username);
                        } catch (Exception e) {
                            System.out.println("Failed to send WordsFetchPack to client: " + e);
                        }
                    }
                } else if(dataPackage instanceof LobbyLeavePack) {
                    try {
                        leaveLobby();
                        LobbyLeavePack toSend = new LobbyLeavePack(true);
                        this.sendDataPackage(toSend, MessageType.LOBBY_LEAVE);
                        System.out.println("Sent LobbyLeavePack success to " + this.username);
                    } catch(Exception ioe) {
                        System.out.println("Failed to notify clients that someone left lobby.");
                    }
                }
            }
        }

        private void leaveLobby() throws IOException {
            if(this.isInLobby()) {
                this.getCurrentLobby().removeClient(this);
                if(this.getCurrentLobby().getClients().size() == 0) {
                    this.getCurrentLobby().stop();
                    lobbies.removeLobby(this.getCurrentLobby());
                } else {
                    this.getCurrentLobby().notifyAll(new LobbyLeavePack(this.username), this, MessageType.LOBBY_LEAVE);
                }
                this.currentLobby = null;
            }
        }

        // Sends data package to client socket
        protected void sendDataPackage(DataPackage dp, MessageType type) throws IOException {
            this.bufferDataPackage(dp, type);
            this.writer.flush();
        }

        /* THIS ONLY "Buffers" the data we're sending. You still need to FLUSH the writer */
        private void bufferDataPackage(DataPackage dp, MessageType type) throws IOException {
            /*
            (First 4 Bytes): length of data represented by 4 bytes
            (length Bytes): json
            {
                "type": "type of data package",
                "data": {
                    // Actual Package data
                }
            }

            THIS IS HANDLED BY RawPacketHandler.writeRawPacket()
            */

            JSONObject dataEncapsulator = new JSONObject();
            dataEncapsulator.put("type", type.toString());
            dataEncapsulator.put("data", dp.toJSON());
            RawPacketHandler.bufferRawPacket(this.writer, dataEncapsulator.toString());
        }

        protected String getUsername() { return this.username; }
        protected String getId() { return this.socketId; }
        protected Lobby getCurrentLobby() { return this.currentLobby; }
        protected boolean isInLobby() { return this.currentLobby != null; }
        protected boolean isVerified() { return this.verified; }

        private void setThisRunnableWrapper(Thread t) { this.thisRunnableWrapper = t; }
    }

}