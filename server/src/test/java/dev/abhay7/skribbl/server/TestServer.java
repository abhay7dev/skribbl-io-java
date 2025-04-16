package dev.abhay7.skribbl.server;

import dev.abhay7.skribbl.server.datapacks.*;

import org.junit.jupiter.api.Test;

import java.net.*;
import java.util.ArrayList;
import java.io.*;

public class TestServer {
    
    String inetadd = "localhost";
    int PORT = 8080;

    @Test
    public void testConnectivity() throws Exception {

        Socket s = new Socket(inetadd, PORT);

        ObjectOutputStream writer = new ObjectOutputStream(s.getOutputStream());
        ObjectInputStream reader = new ObjectInputStream(s.getInputStream());

        System.out.println("Testing server connectivity");

        boolean createdPubLob = false;
        // boolean createdPrivLob = false;

        try {
            Object serverResponse;
            ArrayList<String> users = new ArrayList<>();
            
            while (((serverResponse = reader.readObject()) != null)) {
            
                if(serverResponse instanceof ClientVerificationPack) {
                    ClientVerificationPack cvp = (ClientVerificationPack) serverResponse;
                    System.out.println("Received cvp: " + cvp.getVerificationString());
                
                    ClientVerificationPack newCVP = new ClientVerificationPack(cvp.getVerificationString() + "_VERIFIEDCONNECTION");

                    writer.writeObject(newCVP);
                    writer.flush();
                    System.out.println("Sent new CVP");

                    writer.writeObject(new LobbyListPack());
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
                        writer.writeObject(lipub);
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
                        writer.writeObject(new LobbyListPack());
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
            try {
                boolean isInLobby = false;
                Socket s = new Socket(inetadd, PORT);

                ObjectOutputStream writer = new ObjectOutputStream(s.getOutputStream());
                ObjectInputStream reader = new ObjectInputStream(s.getInputStream());

                Object serverResponse;
                ArrayList<String> users = new ArrayList<>();
                
                while (((serverResponse = reader.readObject()) != null)) {
                
                    if(serverResponse instanceof ClientVerificationPack) {
                        ClientVerificationPack cvp = (ClientVerificationPack) serverResponse;
                        System.out.println(name + ":  Received cvp: " + cvp.getVerificationString());
                    
                        ClientVerificationPack newCVP = new ClientVerificationPack(cvp.getVerificationString() + "_VERIFIEDCONNECTION");

                        writer.writeObject(newCVP);
                        writer.flush();
                        System.out.println(name + ":  Sent new CVP");

                        writer.writeObject(new LobbyListPack());
                        writer.flush();
                    } else if(serverResponse instanceof LobbyListPack) {
                        System.out.println(name + ":  Received lobbylist pack");
                        LobbyListPack l = (LobbyListPack) serverResponse;
                        l.getLobbies().forEach((lob) -> {
                            System.out.println("\t" + name + ": " + lob[0] + "; Private - " + lob[1]);
                        });
                        System.out.println(name + ":  Attempting to join lobby Public Lobby");
                        JoinLobbyPack jlp = new JoinLobbyPack(name, "Public Lobby");
                        writer.writeObject(jlp);
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
            } catch (EOFException e) {
                System.out.println(name + ":  EOFException: Disconnected");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}