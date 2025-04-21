package dev.abhay7.skribbl.client;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.json.JSONObject;

import dev.abhay7.skribbl.server.MessageType;
import dev.abhay7.skribbl.server.RawPacketHandler;
import dev.abhay7.skribbl.server.datapacks.*;

public class Client extends JFrame {

    private static final double ASPECT_RATIO = 16.0/9.0;
    private static final int HEIGHT = 720;
    private static final int WIDTH = (int) (HEIGHT * ASPECT_RATIO);
    private static final Dimension WINDOW_SIZE = new Dimension(WIDTH, HEIGHT);

    private String username;
    private String serverInet;
    private int serverPort;

    private Socket socket;
    private InputStream reader;
    private OutputStream writer;

    private Thread networkThread;
    private boolean runningProgram;

    private JPanel currentPanel;

    private boolean isInLobby;
    private boolean isHost;
    private ArrayList<String> usersInGame;

    private int openDialogs = 0;

    public Client(String[] args) {
        if(args.length < 3 || args[0].isBlank() || args[1].isBlank() || args[2].equals("0")) {
            System.err.println("Invalid client arguments");
            System.exit(1);
        }

        runningProgram = true;

        this.username = args[0];
        this.serverInet = args[1];

        try {
            this.serverPort = Integer.parseInt(args[2]);
            socket = new Socket(this.serverInet, this.serverPort);
            reader = socket.getInputStream();
            writer = socket.getOutputStream();

            String innerDataString = readJSONDataToString(MessageType.CLIENT_VERIFICATION);
            ClientVerificationPack responseVerification = ClientVerificationPack.fromJSON(innerDataString);
            responseVerification = new ClientVerificationPack(responseVerification.getVerificationString() + "_VERIFIEDCONNECTION", this.username);
            sendDataPackage(responseVerification, MessageType.CLIENT_VERIFICATION);

            String confirmString = readJSONDataToString(MessageType.CLIENT_VERIFICATION);
            ClientVerificationPack confirmPack = ClientVerificationPack.fromJSON(confirmString);
            if(!confirmPack.isSuccess()) throw new Exception("Failure");

        } catch(Exception e) {
            showMessageDialog("Failed to connect to server. Your username may have been taken or your internet may be down.", "Network Error", JOptionPane.ERROR_MESSAGE, false);
            runningProgram = false;
        }

        if(runningProgram) {
            networkThread = new Thread(new ClientNetworkHandler());

            this.setTitle("skribbl.io (Java) - Lobbies - " + this.serverInet + ":" + this.serverPort);
            this.setSize(Client.WINDOW_SIZE);
            this.setMinimumSize(Client.WINDOW_SIZE);
            this.setMaximumSize(Client.WINDOW_SIZE);
            this.setResizable(false);
            this.setLocationRelativeTo(null);

            this.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    exitProgram();
                }
            });

            currentPanel = getLobbiesPanel();
            this.add(currentPanel);

            networkThread.start();

            this.isInLobby = false;
            this.isHost = false;
            usersInGame = new ArrayList<String>();

            this.setVisible(true);
        } else {
            exitProgram();
        }
    }

    
    private JPanel getLobbiesPanel() {
        // Overall Panel covering the entire window
        JPanel toRet = new JPanel(new BorderLayout());
        toRet.setSize(WINDOW_SIZE);
        
        // Top panel containing lobby list information
        JPanel header = new JPanel(new BorderLayout());
        header.setSize(WIDTH, HEIGHT / 10);
        
        // Text for header
        JLabel headerText = new JLabel("Lobbies at " + this.serverInet + ":" + this.serverPort);
        headerText.setFont(headerText.getFont().deriveFont(20.0f));
        headerText.setHorizontalAlignment(JLabel.CENTER);
        header.add(headerText, BorderLayout.CENTER);
        
        // Add username top the top right of the header
        JLabel usernameLabel = new JLabel("Logged in as: " + this.username + "  ");
        usernameLabel.setHorizontalAlignment(JLabel.RIGHT);
        header.add(usernameLabel, BorderLayout.NORTH);
        
        // Add header to the NORTH of the panel
        toRet.add(header, BorderLayout.NORTH);
        
        // Lobbies list panel to hold lobbies in the menu
        JPanel lobbiesListPanel = new JPanel();
        lobbiesListPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        Dimension lobbiesDims = new Dimension(WIDTH * 8 / 10, HEIGHT * 8 / 10);
        
        // lobbiesListPanel.setSize(lobbiesDims);
        // lobbiesListPanel.setMinimumSize(lobbiesDims);
        lobbiesListPanel.setPreferredSize(lobbiesDims);
        lobbiesListPanel.setMaximumSize(lobbiesDims);
        lobbiesListPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        updateLobbiesListPanel(lobbiesListPanel, lobbiesDims);
        
        // Add lobbies list to WEST of panel
        toRet.add(lobbiesListPanel, BorderLayout.WEST);
        
        // Options panel to hold all the panels
        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.PAGE_AXIS));
        options.setSize(WIDTH / 8, HEIGHT);
        options.setBorder(new EmptyBorder(0, 0, 0, 10));
        
        // Create buttons array and add action listeners to respond to clicks.
        JButton[] buttons = new JButton[4];
        
        // Create Lobby button
        buttons[0] = new JButton("Create Lobby");
        buttons[0].addActionListener((e) -> { createLobby(); });
        
        // Refresh button to reacquire lobbies
        buttons[1] = new JButton("Refresh Lobbies");
        buttons[1].addActionListener((ae) -> { updateLobbiesListPanel(lobbiesListPanel, lobbiesDims); });
        
        // About Button to give info about program
        buttons[2] = new JButton("About Skribbl");
        buttons[2].addActionListener((ae) -> {
            showMessageDialog("\"skribbl.io is a free online multiplayer drawing and guessing pictionary game.\" This program is a remake of the famous game in Java with Swing/Sockets.\nCreated by Abhay, Sameer, and James.", "About Skribbl", JOptionPane.INFORMATION_MESSAGE);
        });
        
        // Quit Program button
        buttons[3] = new JButton("Quit Skribbl");
        buttons[3].addActionListener((ae) -> { exitProgram(); });
        
        // Add buttons to options panel
        for(JButton button: buttons) {
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            button.setPreferredSize(new Dimension(WIDTH / 9, HEIGHT / (buttons.length + 10)));
            button.setMinimumSize(new Dimension(WIDTH / 9, HEIGHT / (buttons.length + 10)));
            button.setMaximumSize(new Dimension(WIDTH / 9, HEIGHT / (buttons.length + 10)));
            options.add(button);
            options.add(Box.createVerticalGlue());
        }
        
        // Add options panel to the EAST of the Parent panel
        toRet.add(options, BorderLayout.EAST);
        
        return toRet;
    }
    
    // Updates the lobbiestListPanel with the lobbies after fetching stuff
    private void updateLobbiesListPanel(JPanel lobbiesListPanel, Dimension d) {
        SwingUtilities.invokeLater(() -> {
            lobbiesListPanel.removeAll();
            for(JComponent j: getLobbiesListPanel(d)) {
                lobbiesListPanel.add(j);
            }
            lobbiesListPanel.revalidate();
            lobbiesListPanel.repaint();
        });
    }
    
    // Gets buttons to join lobbies if they are open, or display that there are no lobbies currently.
    private ArrayList<JComponent> getLobbiesListPanel(Dimension d) {
        ArrayList<JComponent> components = new ArrayList<>();
        ArrayList<String[]> lobs = getLobbies();
        
        if (lobs == null || lobs.isEmpty()) {
            JLabel label = new JLabel(lobs == null ? "Failed to get lobbies" : "No lobbies currently");
            label.setMinimumSize(new Dimension((int) d.getWidth(), (int) d.getHeight() / 8));
            label.setFont(label.getFont().deriveFont(30.0f));
            label.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            components.add(label);
        } else {
            for(String[] lob: lobs) {
                JButton jb = new JButton();
                jb.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 5));
                jb.add(new JLabel(lob[2]));
                jb.add(new JLabel(lob[0]));
                jb.add(new JLabel("Players: "+ lob[1]));
                jb.setAlignmentX(JButton.CENTER_ALIGNMENT);
                jb.setPreferredSize(new Dimension((int) (d.getWidth() / 3.5), (int) d.getHeight() / 7));
                jb.addActionListener((ae) -> {
                    joinPublicLobby(lob[0]);
                });
                components.add(jb);
            }
        }
        
        return components;
    }
    
    // Network Request to get lobbies from server. Returns ArrayList of [Lobby Name, Player Count, Hostname]
    private ArrayList<String[]> getLobbies() {
        ArrayList<String[]> toRet = new ArrayList<>();
        LobbyListPack llp = new LobbyListPack();
        try {
            sendDataPackage(llp, MessageType.LOBBY_LIST);
            
            String dataString = readJSONDataToString(MessageType.LOBBY_LIST);
            
            llp = LobbyListPack.fromJSON(dataString);
            toRet = llp.getLobbies();
        } catch(Exception e) {
            llp = null;
            JOptionPane.showMessageDialog(this, "Failed to fetch list of lobbies. Try refreshing or restarting the app. The server may also be corrupt.", "Failed to Fetch Lobbies", JOptionPane.ERROR_MESSAGE);
        }
        return toRet;
    }

    private void joinPublicLobby(String lobbyName) {
        LobbyJoinPack ljp = joinLobbyRequest(lobbyName);
        if(ljp != null) {
            this.isInLobby = true;
            this.isHost = false;
            usersInGame.add(this.username);
            usersInGame.addAll(ljp.getPlayers());
            SwingUtilities.invokeLater(() -> {
                this.remove(currentPanel);
                currentPanel = getGamePanel(lobbyName, this.usersInGame, ljp.isStarted());
                this.add(currentPanel);
                this.repaint();
            });
        }
    }
    // Network request to create a lobby
    private LobbyJoinPack joinLobbyRequest(String lobName) {
        
        LobbyJoinPack ljp = new LobbyJoinPack(lobName);
        try {
            sendDataPackage(ljp, MessageType.LOBBY_JOIN);
            String json = readJSONDataToString(MessageType.LOBBY_JOIN);
            ljp = LobbyJoinPack.fromJSON(json);
            return (ljp.isSuccess() ? ljp : null);
        } catch(Exception e) {
            e.printStackTrace();
            showMessageDialog("Failed to send lobby join request", "Network Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }
    

    // Create Lobby and display the game panel
    private void createLobby() {
        String[] lobbyArgs = promptForCreateLobbyArgs();
        if(lobbyArgs != null) {
            boolean joined = createLobbyRequest(lobbyArgs);
            if(joined) {
                this.isInLobby = true;
                this.isHost = true;
                usersInGame.add(this.username);
                SwingUtilities.invokeLater(() -> {
                    this.remove(currentPanel);
                    currentPanel = getGamePanel(lobbyArgs[0]);
                    this.add(currentPanel);
                    this.repaint();
                });
            }
        }
    }
    
    // Network request to create a lobby
    private boolean createLobbyRequest(String[] args) {
        if(args.length == 1) {
            LobbyInitPack lip = new LobbyInitPack(args[0]);
            try {
                sendDataPackage(lip, MessageType.LOBBY_INIT);
                String json = readJSONDataToString(MessageType.LOBBY_INIT);
                lip = LobbyInitPack.fromJSON(json);
                return lip.isSuccess();
            } catch(Exception e) {
                showMessageDialog("Failed to send lobby creation request", "Network Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else if(args.length == 2) {
            // TODO: Implement private lobbies
            try {
                return false;
            } catch(Exception e) {
                showMessageDialog("Failed to send lobby creation request", "Network Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        showMessageDialog("Invalid argument number to createLobby()", "Internal error", JOptionPane.ERROR_MESSAGE);
        return false;
    }
    
    // Prompt user for parameters to create a lobby
    private String[] promptForCreateLobbyArgs() {
        JTextField lobbyNameField = new JTextField("Lobby - " + (new java.util.Date()).toString());
        JCheckBox privateLobbyCheck = new JCheckBox();
        JTextField lobbyPasswordField = new JTextField("");
        
        lobbyPasswordField.setEnabled(false);
        privateLobbyCheck.addActionListener((e) -> {
            lobbyPasswordField.setEnabled(privateLobbyCheck.isSelected());
        });
        
        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
        
        dialogPanel.add(new JLabel("Lobby Name:"));
        dialogPanel.add(lobbyNameField);
        
        dialogPanel.add(Box.createVerticalStrut(10));
        
        JPanel privatePanel = new JPanel();
        privatePanel.setLayout(new BoxLayout(privatePanel, BoxLayout.X_AXIS));
        
        privatePanel.add(new JLabel("Private Lobby:"));
        privatePanel.add(privateLobbyCheck);
        privatePanel.add(Box.createHorizontalStrut(10));
        privatePanel.add(new JLabel("Lobby Password:"));
        privatePanel.add(lobbyPasswordField);
        
        dialogPanel.add(privatePanel);
        
        int result = JOptionPane.showConfirmDialog(this, dialogPanel, "Create a new lobby", JOptionPane.OK_OPTION);
        
        if(result == JOptionPane.OK_OPTION) {
            String lobbyName = lobbyNameField.getText();
            String lobbyPass = lobbyPasswordField.getText();
            
            if(!privateLobbyCheck.isSelected() && !lobbyName.isEmpty()) {
                return new String[]{ lobbyName };
            } else if(privateLobbyCheck.isSelected() && !lobbyName.isEmpty() && !lobbyPass.isEmpty()) {
                return new String[]{ lobbyName, lobbyPass };
            } else {
                showMessageDialog("Invalid input. Please enter a lobby name and if private lobby is selected, a password", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        
        showMessageDialog("User did not proceed with lobby creation.", "Lobby Creation Cancelled", JOptionPane.CANCEL_OPTION);
        return null;
        
    }
    
    // Returns the game panel where actual gameplay will happen
    private JPanel getGamePanel(String lobName) {
        return getGamePanel(lobName, this.usersInGame, false);
    }

    private JPanel getGamePanel(String lobName, ArrayList<String> players, boolean isStarted) {
        JPanel toRet = new JPanel();
        return toRet;
    }

    private void leaveLobby() {

    }
    
    // Helper method to read json to a string
    private String readJSONDataToString(MessageType mt) throws Exception {
        String receivedJsonString = RawPacketHandler.readRawPacket(reader);
        
        JSONObject receivedJsonObject = new JSONObject(receivedJsonString);
        String type = receivedJsonObject.getString("type");
        
        if(MessageType.valueOf(type) != mt) {
            throw new Exception("Invalid MessageType in request");
        }
        
        return receivedJsonObject.getJSONObject("data").toString();
    }
    
    // Network helper methods, copied from the server
    private void sendDataPackage(DataPackage dp, MessageType type) throws IOException {
        this.bufferDataPackage(dp, type);
        this.writer.flush();
    }
    private void bufferDataPackage(DataPackage dp, MessageType type) throws IOException {
        JSONObject dataEncapsulator = new JSONObject();
        dataEncapsulator.put("type", type.toString());
        dataEncapsulator.put("data", dp.toJSON());
        RawPacketHandler.bufferRawPacket(this.writer, dataEncapsulator.toString());
    }
    
    // Method to exit the program
    private synchronized void exitProgram() {
        try {
            runningProgram = false;
            if(networkThread != null && networkThread.isAlive()) networkThread.join();
            if(writer != null) writer.close();
            if(reader != null) reader.close();
            if(socket != null) socket.close();
        } catch(Exception ex) {
            System.err.println("Error while disconnecting from server: " + ex);
        }
        this.dispose();
        this.setVisible(false);
        System.exit(0);
    }

    // Show Dialog Box Messages
    private void showMessageDialog(String msg, String titleMsg, int errorCode) {
        showMessageDialog(msg, titleMsg, errorCode, true);
    }
    private void showMessageDialog(String msg, String titleMsg, int errorCode, boolean invokeLater) {
        if(invokeLater) {
            SwingUtilities.invokeLater(() -> {
                openDialogs++;
                if(openDialogs < 2) {
                    JOptionPane.showMessageDialog(this, msg, titleMsg, errorCode);
                } else {
                    System.err.println("Too many error dialogs called...");
                    System.exit(1);
                }
                openDialogs--;
            });
        } else {
            openDialogs++;
            if(openDialogs < 2) {
                JOptionPane.showMessageDialog(this, msg, titleMsg, errorCode);
            } else {
                System.err.println("Too many error dialogs called...");
                System.exit(1);
            }
            openDialogs--;
        }
    }

    // Runnable to send ping requests
    private class ClientNetworkHandler implements Runnable {

        private long lastPingRequest = java.time.Instant.now().toEpochMilli();

        @Override
        public void run() {
            while(runningProgram) {
                
                try {
                    if(!socket.isConnected() || socket.isClosed()) {
                        throw new IOException("Socket connection has been lost...");
                    }
                    long now = java.time.Instant.now().toEpochMilli();
                    if(/* !socket.isConnected() && */ now - lastPingRequest > 15000) {
                        sendDataPackage(new KeepAlivePack(), MessageType.KEEP_ALIVE);
                        lastPingRequest = now;
                    }
                } catch(IOException ioe) {
                    // ioe.printStackTrace();
                    runningProgram = false;
                    showMessageDialog("Failed to send keep alive request, Program will end shortly...", "Network Error", JOptionPane.ERROR_MESSAGE);
                    System.exit(0);
                }
            }
        }

    }
    
}
