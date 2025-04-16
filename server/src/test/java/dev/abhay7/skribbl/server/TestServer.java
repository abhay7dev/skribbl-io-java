package dev.abhay7.skribbl.server;

import dev.abhay7.skribbl.server.datapacks.*;

import org.junit.jupiter.api.Test;

import java.net.*;
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

        Thread testing = new Thread(() -> {

            boolean createdPubLob = false;
            boolean createdPrivLob = false;

            try {
                Object serverResponse;
                
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
                            LobbyInitPack lipub = new LobbyInitPack("Public Lobby", false);
                            writer.writeObject(lipub);
                            writer.flush();
                            createdPubLob = true;
                        } else if(!createdPrivLob) {
                            System.out.println("Requesting to create a new private lobby");
                            LobbyInitPack lipriv = new LobbyInitPack("Priv Lobby", true);
                            writer.writeObject(lipriv);
                            writer.flush();
                            createdPrivLob = true;
                        }
                        // if(createdPrivLob && createdPubLob) break;
                    } else if(serverResponse instanceof LobbyInitPack) {
                        if(((LobbyInitPack) serverResponse).isSuccess()) {
                            System.out.println("Successfully made new lobby");
                            writer.writeObject(new LobbyListPack());
                            writer.flush();
                        } else {
                            
                            System.out.println("Failed to create lobby");
                        }
                    } else {
                        System.out.println("Received unknown datapack");
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        testing.start();

        System.out.println("Waiting for 10 seconds before ending program");

        Thread.sleep(10000);
        testing.join();

        writer.close();
        reader.close();
        s.close();

    }

}