package dev.abhay7.skribbl.client;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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

    private boolean inLobby;
    private boolean inGame;
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

            this.inLobby = false;
            this.inGame = false;
            usersInGame = new ArrayList<String>();

            this.setVisible(true);
        } else {
            exitProgram();
        }
    }

    private JPanel getHoldingRoomPanel() {
        JPanel toRet = new JPanel(new BorderLayout());
        toRet.setSize(WINDOW_SIZE);

        toRet.add(new JLabel("hi"), BorderLayout.NORTH);

        return toRet;
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

        JPanel lobbiesListPanel = new JPanel();
        lobbiesListPanel.setSize(WIDTH * 8 / 10, HEIGHT * 8 / 10);
        lobbiesListPanel.setMinimumSize(new Dimension(WIDTH * 8 / 10, HEIGHT * 8 / 10));
        lobbiesListPanel.setMaximumSize(new Dimension(WIDTH * 8 / 10, HEIGHT * 8 / 10));
        lobbiesListPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        SwingUtilities.invokeLater(() -> {
            lobbiesListPanel.add(getLobbiesScrollPane(new Dimension(WIDTH * 8 / 10, HEIGHT * 8 / 10)));
            lobbiesListPanel.revalidate();
            lobbiesListPanel.repaint();
        });

        toRet.add(lobbiesListPanel, BorderLayout.WEST);

        // Options panel to hold all the panels
        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.PAGE_AXIS));
        options.setSize(WIDTH / 9, HEIGHT);
        options.setBorder(new EmptyBorder(0, 0, 0, 10));

        // Create buttons array and add action listeners to respond to clicks.
        JButton[] buttons = new JButton[5];
        buttons[0] = new JButton("Join Lobby");
        buttons[1] = new JButton("Create Lobby");
        buttons[1].addActionListener((e) -> {
            String[] lobbyArgs = promptForCreateLobbyArgs();
            if(lobbyArgs != null) {
                boolean joined = createLobby(lobbyArgs);
                if(joined) {
                    inLobby = true;
                    SwingUtilities.invokeLater(() -> {
                        this.remove(currentPanel);
                        currentPanel = getHoldingRoomPanel();
                        this.add(currentPanel);
                        this.repaint();
                    });
                }
            }
        });

        buttons[2] = new JButton("Refresh Lobbies List");
        buttons[2].addActionListener((ae) -> {
            SwingUtilities.invokeLater(() -> {
                lobbiesListPanel.removeAll();
                lobbiesListPanel.add(getLobbiesScrollPane(new Dimension(WIDTH * 8 / 10, HEIGHT * 8 / 10)));
                lobbiesListPanel.revalidate();
                lobbiesListPanel.repaint();
            });
        });

        buttons[3] = new JButton("About Skribbl");
        buttons[3].addActionListener((ae) -> {
            showMessageDialog("\"skribbl.io is a free online multiplayer drawing and guessing pictionary game.\" This program is a remake of the famous game in Java with Swing/Sockets.\nCreated by Abhay, Sameer, and James.", "About Skribbl", JOptionPane.INFORMATION_MESSAGE);
        });
        
        buttons[4] = new JButton("Quit Skribbl");
        buttons[4].addActionListener((ae) -> { exitProgram(); });

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

    private boolean createLobby(String[] args) {
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

    private JScrollPane getLobbiesScrollPane(Dimension d) {
        JPanel listContent = new JPanel();
        listContent.setLayout(new BoxLayout(listContent, BoxLayout.Y_AXIS));

        ArrayList<String[]> lobs = getLobbies();

        if (lobs == null || lobs.isEmpty()) {
            JLabel label = new JLabel(lobs == null ? "Failed to get lobbies" : "No lobbies currently");
            label.setFont(label.getFont().deriveFont(30.0f));
            label.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            listContent.add(label);

            JScrollPane scrollPane = new JScrollPane(listContent);
            scrollPane.setPreferredSize(d);
            return scrollPane;
        }
        JScrollPane jsp = new JScrollPane();

        // TODO: Implement Lobbies Screen

        return jsp;
        
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

    private String readJSONDataToString(MessageType mt) throws Exception {
        String receivedJsonString = RawPacketHandler.readRawPacket(reader);

        JSONObject receivedJsonObject = new JSONObject(receivedJsonString);
        String type = receivedJsonObject.getString("type");
        
        if(MessageType.valueOf(type) != mt) {
            throw new Exception("Invalid MessageType in request");
        }

        return receivedJsonObject.getJSONObject("data").toString();
    }

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
