package dev.abhay7.skribbl.client;

// import java.nio.charset.StandardCharsets;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatIntelliJLaf;

// import dev.abhay7.skribbl.client.jameskwong.pwdsignal.PWDSignalSession;

public class Main {

    public static void main(String[] args) {
        if (true) {
            SwingUtilities.invokeLater(() -> {
                FlatIntelliJLaf.registerCustomDefaultsSource("style");
                FlatIntelliJLaf.setup();
                String[] gameArgs = promptForStartupData();
                if(gameArgs.length < 3 || gameArgs[2].equals("0")) System.exit(0);
                new Client(gameArgs);
            });
            return;
        }

        // try {
        //     System.out.println("TEST CASE 1 -- NORMAL OPERATIONS");
        //     PWDSignalSession alice = new PWDSignalSession("Test123", true);
            
        //     PWDSignalSession bob = new PWDSignalSession("Test123", false);

        //     byte[] alicePayload1 = alice.createPayload1();
        //     byte[] bobPayload1 = bob.createPayload1();
            
        //     alice.acceptPayload1(bobPayload1, 0);
        //     bob.acceptPayload1(alicePayload1, 0);

        //     // payload 1 done

        //     byte[] alicePayload2 = alice.createPayload2();
        //     byte[] bobPayload2 = bob.createPayload2();
            
        //     alice.acceptPayload2(bobPayload2, 0);
        //     bob.acceptPayload2(alicePayload2, 0);

        //     // Payload 2 done
            
        //     byte[] alicePayload3 = alice.createPayload3();
        //     byte[] bobPayload3 = bob.createPayload3();
            
        //     alice.acceptPayload3(bobPayload3, 0);
        //     bob.acceptPayload3(alicePayload3, 0);

        //     System.out.println("Alice state: " + alice.getState());
        //     System.out.println("Bob state: " + bob.getState());    
            
        //     System.out.println();

        //     String message = "Hi bob! I'm alice";
        //     byte[] messageBytes = message.getBytes();
        //     byte[] messageBytesEncrypted = alice.encryptSendPacket(messageBytes, 0, messageBytes.length);

        //     System.out.println("Original message: " + new String(messageBytes, StandardCharsets.UTF_8));
        //     System.out.println();
        //     System.out.println("Encrypted message: " + new String(messageBytesEncrypted, StandardCharsets.UTF_8));
        //     System.out.println();

        //     byte[] messageBytesDecrypted = bob.decryptReceivePacket(messageBytesEncrypted, 0, messageBytesEncrypted.length);
        //     System.out.println("Bob's POV: " + new String(messageBytesDecrypted, StandardCharsets.UTF_8));
        //     System.out.println();

        //     message = "Yo whats good Alice?";
        //     messageBytes = message.getBytes();
        //     messageBytesEncrypted = bob.encryptSendPacket(messageBytes, 0, messageBytes.length);

        //     System.out.println("Original message: " + new String(messageBytes, StandardCharsets.UTF_8));
        //     System.out.println();
        //     System.out.println("Encrypted message: " + new String(messageBytesEncrypted, StandardCharsets.UTF_8));
        //     System.out.println();

        //     messageBytesDecrypted = alice.decryptReceivePacket(messageBytesEncrypted, 0, messageBytesEncrypted.length);
        //     System.out.println("Alice's POV: " + new String(messageBytesDecrypted, StandardCharsets.UTF_8));
        //     System.out.println();

        //     System.out.println("\n\nTEST CASE 2 -- REFLECTION ATTACK");

        //     // attacker replays Bob's message back to him

        //     try {
        //         byte[] reflectionAttack = bob.decryptReceivePacket(messageBytesEncrypted, 0, messageBytesEncrypted.length);
        //     }
        //     catch (Exception e) {
        //         System.out.println("Error ocurred during Reflection attack occurred: " + e.getMessage());
        //     }

        //     System.out.println("\n\nTEST CASE 3 -- REPLAY ATTACK");


        //     try {
        //         byte[] replayAttack = alice.decryptReceivePacket(messageBytesEncrypted, 0, messageBytesEncrypted.length);
        //     }
        //     catch (Exception e) {
        //         System.out.println("Error ocurred during replay attack occurred: " + e.getMessage());
        //     }

        //     System.out.println("\n\nTEST CASE 4 -- WRONG PASSWORD");

        //     alice = new PWDSignalSession("Test123", true);
            
        //     bob = new PWDSignalSession("Bruh", false);

        //     alicePayload1 = alice.createPayload1();
        //     bobPayload1 = bob.createPayload1();
            
        //     alice.acceptPayload1(bobPayload1, 0);
        //     bob.acceptPayload1(alicePayload1, 0);

        //     // payload 1 done

        //     alicePayload2 = alice.createPayload2();
        //     bobPayload2 = bob.createPayload2();
            
        //     alice.acceptPayload2(bobPayload2, 0);
        //     bob.acceptPayload2(alicePayload2, 0);

        //     // Payload 2 done
            
        //     alicePayload3 = alice.createPayload3();
        //     bobPayload3 = bob.createPayload3();
            
        //     alice.acceptPayload3(bobPayload3, 0);
        //     bob.acceptPayload3(alicePayload3, 0);

        //     System.out.println("Alice state: " + alice.getState());
        //     System.out.println("Bob state: " + bob.getState());    
            
        //     System.out.println();
        // }
        // catch (Exception e) {
        //     System.out.println("Error occurred during wrong password attack: " + e.getMessage());
        // }
    }

    private static String[] promptForStartupData() {
        String[] toRet = new String[3];
        toRet[0] = ""; toRet[1] = ""; toRet[2] = "0";

        JTextField usernameField = new JTextField("Username-" + ((char) (((int) (Math.random() * 26)) + 65)) + ((char) (((int) (Math.random() * 26)) + 65)) + ((char) (((int) (Math.random() * 26)) + 65)));
        JTextField serverField = new JTextField("localhost:8080");

        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
        
        dialogPanel.add(new JLabel("Username:"));
        dialogPanel.add(usernameField);

        dialogPanel.add(Box.createVerticalStrut(10));
        
        dialogPanel.add(new JLabel("Server (IP:PORT):"));
        dialogPanel.add(serverField);

        int result = JOptionPane.showConfirmDialog(null, dialogPanel, "Connect to a server", JOptionPane.OK_OPTION);

        if(result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            String serverInput = serverField.getText().trim();

            if (!username.isEmpty() && serverInput.matches("^.+:\\d+$") && username.length() < 15 && username.indexOf(":") < 0) {
                String[] parts = serverInput.split(":");
                
                toRet[0] = username;
                toRet[1] = parts[0];
                toRet[2] = Integer.parseInt(parts[1]) + "";
            } else {
                JOptionPane.showMessageDialog(null, "Invalid input. Please enter a username (less than 15 characters) without a : and server in IP:PORT format.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(null, "User did not proceed with connection.");
            System.exit(0);
        }

        return toRet;
    }
    
}
