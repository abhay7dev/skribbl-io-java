package dev.abhay7.skribbl.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONObject;

import dev.abhay7.skribbl.server.datapacks.ClientVerificationPack;
import dev.abhay7.skribbl.server.datapacks.DataPackage;
import dev.abhay7.skribbl.server.datapacks.JoinLobbyPack;
import dev.abhay7.skribbl.server.datapacks.LobbyInitPack;
import dev.abhay7.skribbl.server.datapacks.LobbyListPack;

public class Server {
    // max of 16MB pakcets at once
    private static final int MAX_RAW_PACKET_LENGTH = 1024 * 1024 * 16;

    private ServerSocket serverSocket;
    private boolean isRunning;

    private CopyOnWriteArrayList<Thread> connectedClientList = new CopyOnWriteArrayList<Thread>();
    private CopyOnWriteArrayList<ClientCommsHandler> connectedClientListRunnables = new CopyOnWriteArrayList<ClientCommsHandler>();

    private LobbiesHandler lobbies = new LobbiesHandler();

    public Server(int PORT) {
        try {
            serverSocket = new ServerSocket(PORT);
            isRunning = true;
            System.out.println("Skribbl Server running on port " + PORT + "!");
        } catch (Exception e) {
            System.err.println("Fatal Error. Failed to start Skribbl Server on port " + PORT + "\n" + e);
        }
        runServer();
    }

    public void runServer() {
        while (isRunning) {

            Socket clientSocket = null;
            OutputStream writer = null;
            InputStream reader = null;

            try {
                clientSocket = serverSocket.accept();

                // thisi s  TCP scoket
                // socket(... SOCKET_TCP)
                
                InputStream inputStream = clientSocket.getInputStream();
                OutputStream outputStream = clientSocket.getOutputStream();

                writer = outputStream;
                reader = inputStream;
                // writer = new ObjectOutputStream(clientSocket.getOutputStream());
                // reader = new ObjectInputStream();
            } catch (Exception e) {
                System.err.println("Failed to accept client socket.");
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
        private boolean inLobby = false;
        private String socketId = "";
        private String username = "";

        private ClientCommsHandler(Socket cs, OutputStream writer, InputStream reader) {
            this.clientSocket = cs;
            this.writer = writer;
            this.reader = reader;
            this.socketId = java.time.Instant.now().toEpochMilli() + "_" + Math.random();
        }

        @Override
        public void run() {
            ClientVerificationPack cvp = new ClientVerificationPack(Math.random() + "");

            int waittime = 10000;
            String verifyString = "_VERIFIEDCONNECTION";

            String jsonString;

            try {
                // writer.writeObject(cvp);
                // writer.flush();
                sendDP(cvp, MessageType.CLIENT_VERIFICATION_REQUEST);
                writer.flush();

                this.clientSocket.setSoTimeout(waittime);

                jsonString = readJsonRawPacket();
                // ClientVerificationPack responseVerification = (ClientVerificationPack) this.reader.readObject();
                // interpret it as a client verificaiton pack which we *have* to do
                
                // you can either store the *type* of data that's being sent as raw integer (like 4 byte integer) or as a 
                // field inside the actual JSON object itself
                // like :

                // {
                //  type: ''
                // }

                ClientVerificationPack responseVerification = ClientVerificationPack.fromJSON(jsonString);


                if (!responseVerification.getVerificationString().equals(cvp.getVerificationString() + verifyString))
                    throw new Exception("Invalid verification response");
                else {
                    System.out.println("Client successfully verified");
                    this.verified = true;
                    this.clientSocket.setSoTimeout(waittime * 3);
                }

            } catch (Exception ste) {
                System.out.println("A client failed to respond to verification packet in time, will be removed.");
                this.disconnectAndTerminateUser();
            }

            DataPackage receivedClientData;

            try {
                

                // while (((receivedClientData = (DataPackage) reader.readObject())) != null) {
                //     handleDataPackage(receivedClientData);
                // }
                while (true) {
                    jsonString = readJsonRawPacket();
                    JSONObject obj = new JSONObject(jsonString);
                    String type = obj.getString("type");
                    JSONObject innerData = obj.getJSONObject("data");
                    String innerDataString = innerData.toString();
                    
                    MessageType mt = MessageType.valueOf(type);
                    switch (mt) {
                        case LOBBY_INIT:
                            receivedClientData = LobbyInitPack.fromJSON(innerDataString);
                            break;
                        case LOBBY_JOIN:
                            receivedClientData = JoinLobbyPack.fromJSON(innerDataString);
                            break;
                        case LOBBY_LIST:
                            receivedClientData = LobbyListPack.fromJSON(innerDataString);
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
                // this path right here
                // i guess thing to improve is this is suscpetible to frame-shifts
                // so there's no 'rolling window search' for a correct packet header
                // but that's fine ; let's just assume that complete poackets will be sent
                // regarding security, in the case of bum data being sent, the server will go to this path and can crash or stop or whatever
                // TODO: kick client
                System.out.println("Error in recieving client data: " + e);
            } finally {
                this.disconnectAndTerminateUser();
            }

        }

        // reads count bytes into arr starting at index offset
        private void readNBytes(InputStream is, byte[] arr, int offset, int count) throws IOException {
            int read = 0;
            while (read < count) {
                int adder = is.read(arr, offset + read, count - read);
                if (adder == -1) {
                    throw new IOException("EOF");
                }
                read += adder;
            }
        }

        private String readJsonRawPacket() throws Exception {
            // this represents the length of the JSON packet
            byte[] lengthBytesLE = new byte[4];
            readNBytes(reader, lengthBytesLE, 0, 4);
            int length = littleEndianBytesToInt(lengthBytesLE, 0);

            // we have the lenght of hte JSON data!

            if (length > MAX_RAW_PACKET_LENGTH) {
                throw new Exception("Raw packet is way too big: " + length);
            }

            byte[] jsonData = new byte[length];
            readNBytes(reader, jsonData, 0, length);

            // interpret data as UTF-8
            return new String(jsonData, StandardCharsets.UTF_8);
        }

        // when it comes to integers of length > 255, they're stored as more than one
        // byte; the order of these bytes matter
        // for example, you could say the first byte in a 2 byte array is the "LEAST
        // SIGNIFICANT" part of it
        // that's little endian
        public static int littleEndianBytesToInt(byte[] bytes, int offset) {
            if (bytes.length - offset < 4)
                throw new IllegalArgumentException("Byte array too short (must be at least 4 bytes)");

            // 1111 0000 1111 0000 1111 0000 1111 0000
            // [byte 1] [byte 2] [byte 3] [byte 4]

            // 0xFF is needed as a sign extension
            return (bytes[0] & 0xFF) |
                    ((bytes[1] & 0xFF) << 8) |
                    ((bytes[2] & 0xFF) << 16) |
                    ((bytes[3] & 0xFF) << 24);
        }

        public void writeNBytes(byte[] arr, int offset, int count) throws IOException {
            int written = 0;
            while (written < count) {
                writer.write(arr, offset + written, count - written);
                written = count; // OutputStream.write(byte[],off,len) blocks until all len bytes are written
            }
        }

        public static void intToLittleEndianBytes(int value, byte[] bytes, int offset) {
            if (bytes.length - offset < 4) {
                throw new IllegalArgumentException("Byte array too short (need 4 bytes at offset)");
            }
            bytes[offset] = (byte) (value & 0xFF);
            bytes[offset + 1] = (byte) ((value >> 8) & 0xFF);
            bytes[offset + 2] = (byte) ((value >> 16) & 0xFF);
            bytes[offset + 3] = (byte) ((value >> 24) & 0xFF);
        }

        private void setThisRunnableWrapper(Thread t) {
            this.thisRunnableWrapper = t;
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
            } catch (Exception e) {
                System.out.println("Failed to disconnect and terminate a user");
            }
        }

        private void handleDataPackage(DataPackage dataPackage) {
            System.out.println("Received Data Package: " + dataPackage);
            if (this.verified && dataPackage.isRequest()) {
                if (dataPackage instanceof LobbyListPack) {
                    LobbyListPack toSend = new LobbyListPack(lobbies.getLobbyArrayList());
                    try {
                        // this.writer.writeObject(toSend);
                        sendDP(toSend, MessageType.LOBBY_LIST);
                        this.writer.flush();
                        System.out.println("Sent LobbyPack with lobbies list");
                    } catch (Exception e) {
                        System.out.println("Failed to send LobbyListPacket to client: " + e);
                    }
                } else if (dataPackage instanceof LobbyInitPack) {
                    LobbyInitPack p = (LobbyInitPack) dataPackage;
                    Lobby lob = null;
                    try {
                        if (p.getUsername().equals(null))
                            throw new IllegalArgumentException();
                        this.username = p.getUsername();
                        lob = new Lobby(p, this);
                        lobbies.addLobby(lob);
                        this.inLobby = true;
                        // this.writer.writeObject(new LobbyInitPack(true));
                        sendDP(new LobbyInitPack(true), MessageType.LOBBY_INIT);
                        this.writer.flush();
                    } catch (IllegalArgumentException iae) {
                        System.out.println(iae);
                        try {
                            // this.writer.writeObject(new LobbyInitPack(false));
                            sendDP(new LobbyInitPack(false), MessageType.LOBBY_INIT);
                            this.writer.flush();
                            // After receiving this package, client should initialize its own game. Because
                            // games run on the "host", not the server itself, which only facilitates
                            // connection and communication.
                        } catch (Exception e) {
                            System.out.println("Failed to send failure to client: " + e);
                            lobbies.removeLobby(lob);
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to notify of success creating lobby. Removing lobby...");
                        lobbies.removeLobby(lob);
                    }

                } else if (dataPackage instanceof JoinLobbyPack) {
                    String lobName = ((JoinLobbyPack) dataPackage).getLobbyName();
                    this.username = ((JoinLobbyPack) dataPackage).getUsername();

                    Lobby lob = lobbies.getPublicLobbyByName(lobName);
                    if (lob != null) {

                        try {
                            // this.writer.writeObject(new JoinLobbyPack(true, lob.getPlayerNames()));
                            sendDP(new JoinLobbyPack(true, lob.getPlayerNames()), MessageType.LOBBY_JOIN);
                            lob.addClient(this);
                            this.inLobby = true;
                            this.writer.flush();

                            System.out.println("Notifying all except sender (" + this.username + ")");
                            lob.notifyAllExceptSender(dataPackage, this, MessageType.LOBBY_JOIN);

                        } catch (Exception e) {
                            System.out.println("Failed to notify about joining lobby");
                        }

                    } else {
                        try {
                            // this.writer.writeObject(new JoinLobbyPack(false));
                            sendDP(new JoinLobbyPack(false), MessageType.LOBBY_JOIN);
                            this.writer.flush();
                        } catch (Exception e) {
                            System.out.println("Failed to send failure of joining lobby: " + e);
                        }
                    }
                }
            }
        }

        // protected void sendPackage(DataPackage dp) throws IOException, IllegalAccessError {
        //     if (this.writer != null) {
        //         // writer.writeObject(dp);
                
        //         writer.flush();
        //     } else {
        //         throw new IllegalAccessError("Attempted to access disconnected socket");
        //     }
        // }

        protected void sendDP(DataPackage dp, MessageType type) throws IOException {
            // format:
            // length
            // json
            // has a key called type
            // has key called data
            // inside data is the rendered JSON object

            JSONObject upper = new JSONObject();
            upper.put("type", type.toString());
            
            if (dp instanceof ClientVerificationPack) {
                upper = dp.toJSON();
            }
            else {
                JSONObject inner = dp.toJSON();
                upper.put("data", inner);
            }

            byte[] upperBytes = upper.toString().getBytes(StandardCharsets.UTF_8);

            byte[] lengthBytes = new byte[4];
            intToLittleEndianBytes(upperBytes.length, lengthBytes, 0);

            writeNBytes(lengthBytes, 0, 4);
            writeNBytes(upperBytes, 0, upperBytes.length);
        }

        protected String getUsername() {
            return this.username;
        }

        protected String getId() {
            return this.socketId;
        }

        protected boolean isInLobby() {
            return this.inLobby;
        }

    }

}