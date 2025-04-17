package dev.abhay7.skribbl.server;

import dev.abhay7.skribbl.server.datapacks.*;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;


import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.io.*;

public class TestServer {
    
    String inetadd = "localhost";
    int PORT = 8080;


        // reads count bytes into arr starting at index offset
        private static void readNBytes(InputStream is, byte[] arr, int offset, int count) throws IOException {
            int read = 0;
            while (read < count) {
                int adder = is.read(arr, offset + read, count - read);
                if (adder == -1) {
                    throw new IOException("EOF");
                }
                read += adder;

            }
        }

        private static String readJsonRawPacket(InputStream reader) throws Exception {
            // this represents the length of the JSON packet
            byte[] lengthBytesLE = new byte[4];
            System.out.println("here");
            readNBytes(reader, lengthBytesLE, 0, 4);
            int length = littleEndianBytesToInt(lengthBytesLE, 0);

            System.out.println("JSON PACKET LENGTH: " + length);
            // we have the lenght of hte JSON data!

            if (length > 16 * 1024 * 1024) {
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

        public static void writeNBytes(OutputStream writer, byte[] arr, int offset, int count) throws IOException {
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

    private static DataPackage readStupidDP(InputStream reader)  throws Exception {
        String jsonString = readJsonRawPacket(reader);

        JSONObject obj = new JSONObject(jsonString);
        String type = obj.optString("type");
        if (type.isEmpty()) {
            // CVP
            return ClientVerificationPack.fromJSON(jsonString);
        }

        // not CVP

        JSONObject innerData = obj.getJSONObject("data");
        String innerDataString = innerData.toString();
        
        MessageType mt = MessageType.valueOf(type);
        switch (mt) {
            case LOBBY_INIT:
                return LobbyInitPack.fromJSON(innerDataString);
            case LOBBY_JOIN:
                return JoinLobbyPack.fromJSON(innerDataString);
            case LOBBY_LIST:
                return LobbyListPack.fromJSON(innerDataString);
            default:
                throw new Exception("Unsupported message type");
        }
    }
    private static void sendDP(OutputStream writer, DataPackage dp, MessageType type) throws Exception {
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

        writeNBytes(writer, lengthBytes, 0, 4);
        writeNBytes(writer, upperBytes, 0, upperBytes.length);
    }
    @Test
    public void testConnectivity() throws Exception {

        Socket s = new Socket(inetadd, PORT);

        OutputStream writer = s.getOutputStream();
        InputStream reader = s.getInputStream();

        System.out.println("Testing server connectivity");

        boolean createdPubLob = false;
        // boolean createdPrivLob = false;

        try {
            Object serverResponse;
            ArrayList<String> users = new ArrayList<>();
            
            while (((serverResponse = readStupidDP(reader)) != null)) {
            
                if(serverResponse instanceof ClientVerificationPack) {
                    ClientVerificationPack cvp = (ClientVerificationPack) serverResponse;
                    System.out.println("Received cvp: " + cvp.getVerificationString());
                
                    ClientVerificationPack newCVP = new ClientVerificationPack(cvp.getVerificationString() + "_VERIFIEDCONNECTION");

                    sendDP(writer, newCVP, MessageType.CLIENT_VERIFICATION_REQUEST);
                    // writer.writeObject(newCVP);
                    writer.flush();
                    System.out.println("Sent new CVP");

                    // writer.writeObject(new LobbyListPack());
                    sendDP(writer, new LobbyListPack(), MessageType.LOBBY_LIST);
                    writer.flush();
                } else if(serverResponse instanceof LobbyListPack) {
                    System.out.println("Received lobbylist pack");
                    LobbyListPack l = (LobbyListPack) serverResponse;
                    l.getLobbies().forEach((lob) -> {
                        System.out.println("\t" + lob[0] + "; Private - " + lob[1]);
                    });

                    if(!createdPubLob) {                        
                        System.out.println("Requesting to create a new public lobby");
                        LobbyInitPack lipub = new LobbyInitPack("Public Lobby", "Host", false);
                        users.add("Host");
                        // writer.writeObject(lipub);
                        sendDP(writer, lipub, MessageType.LOBBY_INIT);
                        writer.flush();
                        createdPubLob = true;
                    } else {
                        System.out.println("I should now be a 'host'...\nWill allow a new client to connect in 5 seconds");
                        Thread.sleep(5000);
                        new Thread(new NewClient("Bob")).start();

                    }/* else if(!createdPrivLob) {
                        System.out.println("Requesting to create a new private lobby");
                        LobbyInitPack lipriv = new LobbyInitPack("Priv Lobby", true);
                        writer.writeObject(lipriv);
                        writer.flush();
                        createdPrivLob = true;
                    }*/
                    // if(createdPrivLob && createdPubLob) break;
                } else if(serverResponse instanceof LobbyInitPack) {
                    if(((LobbyInitPack) serverResponse).isSuccess()) {
                        System.out.println("Successfully made new lobby");
                        // writer.writeObject(new LobbyListPack());
                        sendDP(writer, new LobbyListPack(), MessageType.LOBBY_LIST);
                        writer.flush();
                    } else {
                        System.out.println("Failed to create lobby");
                        break;
                    }
                } else if(serverResponse instanceof JoinLobbyPack) {
                    JoinLobbyPack jlp = (JoinLobbyPack) serverResponse;

                    users.add(jlp.getUsername());

                    System.out.println("New Client joined. Updated users list:");
                    System.out.println("Players list:");
                    for(String str: users) {
                        System.out.println("\t" + str);
                    }

                } else {
                    System.out.println("Received unknown datapack");
                }
            }
        } catch (EOFException e) {
            System.out.println("EOFException: Disconnected");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Waiting for 10 seconds before ending program");

        Thread.sleep(10000);
        
        writer.close();
        reader.close();
        s.close();

    }

    private class NewClient implements Runnable {
        private String name;
        public NewClient(String name) {
            this.name = name;
        }

        public void run() {
            Socket s = null;
            OutputStream writer = null;
            InputStream reader = null;
            try {
                boolean isInLobby = false;
                s = new Socket(inetadd, PORT);

                writer = s.getOutputStream();
                reader = s.getInputStream();

                Object serverResponse;
                ArrayList<String> users = new ArrayList<>();
                
                while (((serverResponse = readStupidDP(reader)) != null)) {
                
                    if(serverResponse instanceof ClientVerificationPack) {
                        ClientVerificationPack cvp = (ClientVerificationPack) serverResponse;
                        System.out.println(name + ":  Received cvp: " + cvp.getVerificationString());
                    
                        ClientVerificationPack newCVP = new ClientVerificationPack(cvp.getVerificationString() + "_VERIFIEDCONNECTION");

                        // writer.writeObject(newCVP);
                        sendDP(writer, newCVP, MessageType.CLIENT_VERIFICATION_REQUEST);
                        writer.flush();
                        System.out.println(name + ":  Sent new CVP");

                        // writer.writeObject(new LobbyListPack());
                        sendDP(writer, new LobbyListPack(), MessageType.LOBBY_LIST);
                        writer.flush();
                    } else if(serverResponse instanceof LobbyListPack) {
                        System.out.println(name + ":  Received lobbylist pack");
                        LobbyListPack l = (LobbyListPack) serverResponse;
                        l.getLobbies().forEach((lob) -> {
                            System.out.println("\t" + name + ": " + lob[0] + "; Private - " + lob[1]);
                        });
                        System.out.println(name + ":  Attempting to join lobby Public Lobby");
                        JoinLobbyPack jlp = new JoinLobbyPack(name, "Public Lobby");
                        // writer.writeObject(jlp);
                        sendDP(writer, jlp, MessageType.LOBBY_JOIN);
                        writer.flush();

                    } else if(serverResponse instanceof JoinLobbyPack) {
                        if(!isInLobby) {
                            JoinLobbyPack jlp = (JoinLobbyPack) serverResponse;

                            if(jlp.isSuccess()) {
                                users.add(name);
                                for(String str: jlp.getPlayers()) {
                                    users.add(str);
                                }
                                System.out.println(name + ":  Players list:");
                                for(String str: users) {
                                    System.out.println("\t" + name + ": " + str);
                                }
                                isInLobby = true;
                                if(!users.contains("John")) {
                                    Thread.sleep(5000);
                                    System.out.println("Currently in " + name + ". Waiting 5 seconds before creating next person.");
                                    new Thread(new NewClient("John")).start();
                                }
                            } else {
                                System.out.println(name + ":  Failure in joining server");
                            }
                        } else {
                            JoinLobbyPack jlp = (JoinLobbyPack) serverResponse;

                            users.add(jlp.getUsername());

                            System.out.println(name + ": New Client joined. Updated users list:");
                            System.out.println(name + ": Players list:");
                            for(String str: users) {
                                System.out.println("\t" + name + ": " + str);
                            }
                        }

                    } else {
                        System.out.println(name + ":  Received unknown datapack");
                    }

                }
                writer.close();
                reader.close();
                s.close();
            } catch (EOFException e) {
                System.out.println(name + ":  EOFException: Disconnected");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}