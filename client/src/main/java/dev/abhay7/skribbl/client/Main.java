package dev.abhay7.skribbl.client;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();
            String[] gameArgs = promptForStartupData();
            if(gameArgs.length < 3 || gameArgs[2].equals("0")) System.exit(0);
            new Client(gameArgs);
        });
    }

    private static String[] promptForStartupData() {
        String[] toRet = new String[3];
        toRet[0] = ""; toRet[1] = ""; toRet[2] = "0";

        JTextField usernameField = new JTextField("Username");
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

            if (!username.isEmpty() && serverInput.matches("^.+:\\d+$")) {
                String[] parts = serverInput.split(":");
                
                toRet[0] = username;
                toRet[1] = parts[0];
                toRet[2] = Integer.parseInt(parts[1]) + "";
            } else {
                JOptionPane.showMessageDialog(null, "Invalid input. Please enter a username and server in IP:PORT format.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(null, "User did not proceed with connection.");
            System.exit(0);
        }

        return toRet;
    }
    
}
