package dev.abhay7.skribbl.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.CopyOnWriteArrayList;

import dev.abhay7.skribbl.server.datapacks.ClientVerificationPack;
import dev.abhay7.skribbl.server.datapacks.DataPackage;
import dev.abhay7.skribbl.server.datapacks.JoinLobbyPack;
import dev.abhay7.skribbl.server.datapacks.LobbyInitPack;
import dev.abhay7.skribbl.server.datapacks.LobbyListPack;

public class Server {

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
            ObjectOutputStream writer = null;
            ObjectInputStream reader = null;

            try {
                clientSocket = serverSocket.accept();
                writer = new ObjectOutputStream(clientSocket.getOutputStream());
                reader = new ObjectInputStream(clientSocket.getInputStream());
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
        private ObjectOutputStream writer;
        private ObjectInputStream reader;
        private Thread thisRunnableWrapper;

        private boolean verified = false;
        private boolean inLobby = false;
        private String socketId = "";
        private String username = "";

        private ClientCommsHandler(Socket cs, ObjectOutputStream writer, ObjectInputStream reader) {
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

            try {

                writer.writeObject(cvp);
                writer.flush();
                
                this.clientSocket.setSoTimeout(waittime);
                ClientVerificationPack responseVerification = (ClientVerificationPack) this.reader.readObject();

                if(!responseVerification.getVerificationString().equals(cvp.getVerificationString() + verifyString)) throw new Exception("Invalid verification response");
                else {
                    System.out.println("Client successfully verified");
                    this.verified = true;
                    this.clientSocket.setSoTimeout(waittime * 3);
                }

            } catch(Exception ste) {
                System.out.println("A client failed to respond to verification packet in time, will be removed.");
                this.disconnectAndTerminateUser();
            }

            DataPackage receivedClientData;
            
            try {
                
                while(((receivedClientData = (DataPackage) reader.readObject())) != null) {
                    handleDataPackage(receivedClientData);
                }

            } catch(EOFException e) {
                System.out.println("Client socket disconnected");
            } catch(SocketTimeoutException e) {
                System.out.println("Client socket timed out");
            } catch(Exception e) {
                System.out.println("Error in recieving client data: " + e);
            } finally {
                this.disconnectAndTerminateUser();
            }

        }

        private void setThisRunnableWrapper(Thread t) {
            this.thisRunnableWrapper = t;
        }

        private void disconnectAndTerminateUser() {
            try {
                this.writer.close();
                this.reader.close();
                this.clientSocket.close();
                if(this.thisRunnableWrapper != null) {
                    connectedClientList.remove(this.thisRunnableWrapper);
                    connectedClientListRunnables.remove(this);
                    // thisRunnableWrapper.join();
                }
            } catch(Exception e) {
                System.out.println("Failed to disconnect and terminate a user");
            }
        }

        private void handleDataPackage(DataPackage dataPackage) {
            System.out.println("Received Data Package: " + dataPackage);
            if(this.verified && dataPackage.isRequest()) {
                if(dataPackage instanceof LobbyListPack) {
                    LobbyListPack toSend = new LobbyListPack(lobbies.getLobbyArrayList());
                    try {
                        this.writer.writeObject(toSend);
                        this.writer.flush();
                        System.out.println("Sent LobbyPack with lobbies list");
                    } catch(Exception e) {
                        System.out.println("Failed to send LobbyListPacket to client: " + e);
                    }
                } else if(dataPackage instanceof LobbyInitPack) {
                    LobbyInitPack p = (LobbyInitPack) dataPackage;
                    Lobby lob = null;
                    try {
                        if(p.getUsername().equals(null)) throw new IllegalArgumentException();
                        this.username = p.getUsername();
                        lob = new Lobby(p, this);
                        lobbies.addLobby(lob);
                        this.inLobby = true;
                        this.writer.writeObject(new LobbyInitPack(true));
                        this.writer.flush();
                    } catch(IllegalArgumentException iae) {
                        System.out.println(iae);
                        try {
                            this.writer.writeObject(new LobbyInitPack(false));
                            this.writer.flush();
                            // After receiving this package, client should initialize its own game. Because games run on the "host", not the server itself, which only facilitates connection and communication.
                        } catch(Exception e) {
                            System.out.println("Failed to send failure to client: " + e);
                            lobbies.removeLobby(lob);
                        }
                    } catch(Exception e) {
                        System.out.println("Failed to notify of success creating lobby. Removing lobby...");
                        lobbies.removeLobby(lob);
                    }
                    
                } else if(dataPackage instanceof JoinLobbyPack) {
                    String lobName = ((JoinLobbyPack) dataPackage).getLobbyName();
                    this.username = ((JoinLobbyPack) dataPackage).getUsername();

                    Lobby lob = lobbies.getPublicLobbyByName(lobName);
                    if(lob != null) {

                        try {
                            this.writer.writeObject(new JoinLobbyPack(true, lob.getPlayerNames()));
                            lob.addClient(this);
                            this.inLobby = true;
                            this.writer.flush();

                            System.out.println("Notifying all except sender (" + this.username + ")");
                            lob.notifyAllExceptSender(dataPackage, this);

                        } catch(Exception e) {
                            System.out.println("Failed to notify about joining lobby");
                        }

                    } else {
                        try {
                            this.writer.writeObject(new JoinLobbyPack(false));
                            this.writer.flush();
                        } catch(Exception e) {
                            System.out.println("Failed to send failure of joining lobby: " + e);
                        }
                    }
                }
            }
        }
    
        protected void sendPackage(DataPackage dp) throws IOException, IllegalAccessError {
            if(this.writer != null) {
                writer.writeObject(dp);
                writer.flush();
            } else {
                throw new IllegalAccessError("Attempted to access disconnected socket");
            }            
        }

        protected String getUsername() { return this.username; }
        protected String getId() { return this.socketId; }
        protected boolean isInLobby() { return this.inLobby; }

    }


}
