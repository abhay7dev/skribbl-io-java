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
                            System.out.println(lob[0] + "; Private - " + lob[1]);
                        });
                        break;
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