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

        new Thread(() -> {

            try {
                Object serverResponse;
                
                while (((serverResponse = reader.readObject()) != null)) {
                
                    ClientVerificationPack cvp = (ClientVerificationPack) serverResponse;
                    System.out.println("Received cvp: " + cvp.getVerificationString());
                
                    ClientVerificationPack newCVP = new ClientVerificationPack(cvp.getVerificationString() + "_VERIFIEDCONNECTION");

                    writer.writeObject(newCVP);
                    writer.flush();

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        System.out.println("Waiting for 10 seconds until test is done.");
        Thread.sleep(10000);

        ClientVerificationPack newCVP = new ClientVerificationPack("_VERIFIEDCONNECTION");

        writer.writeObject(newCVP);
        writer.flush();

        Thread.sleep(500);

        writer.writeObject(newCVP);
        writer.flush();

        Thread.sleep(2000);

        s.close();

    }

}