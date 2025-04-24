package dev.abhay7.skribbl.client.unsafe;

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
import java.util.List;

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
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import org.json.JSONObject;

import dev.abhay7.skribbl.server.MessageType;
import dev.abhay7.skribbl.server.RawPacketHandler;
import dev.abhay7.skribbl.server.datapacks.*;

public class Client2 extends JFrame {

    public static final double ASPECT_RATIO = 16.0 / 9.0;
    public static final int HEIGHT = 720;
    public static final int WIDTH = (int) (HEIGHT * ASPECT_RATIO);
    public static final Dimension WINDOW_SIZE = new Dimension(WIDTH, HEIGHT);

    private String username;
    private String serverInet;
    private int serverPort;

    private Socket socket;
    private InputStream reader;
    private OutputStream writer;

    private Thread keepAliveThread;
    private volatile boolean runningProgram;

    private JPanel currentPanel;
    private JPanel usersPanel;

    private volatile boolean isInLobby;
    private volatile boolean isHost;
    private volatile ArrayList<String> usersInGame;

    private int openDialogs = 0;

    public Client2(String[] args) {
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
            keepAliveThread = new Thread(new ClientNetworkHandler());

            this.setTitle("skribbl.io (Java) - Lobbies - " + this.serverInet + ":" + this.serverPort);
            this.setSize(Client2.WINDOW_SIZE);
            this.setMinimumSize(Client2.WINDOW_SIZE);
            this.setMaximumSize(Client2.WINDOW_SIZE);
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

            keepAliveThread.start();

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
        lobbiesListPanel.setPreferredSize(lobbiesDims);
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
        buttons[0].addActionListener((_) -> { createLobby(); });
        
        // Refresh button to reacquire lobbies
        buttons[1] = new JButton("Refresh Lobbies");
        buttons[1].addActionListener((_) -> { updateLobbiesListPanel(lobbiesListPanel, lobbiesDims); });
        
        // About Button to give info about program
        buttons[2] = new JButton("About Skribbl");
        buttons[2].addActionListener((_) -> {
            showMessageDialog("\"skribbl.io is a free online multiplayer drawing and guessing pictionary game.\" This program is a remake of the famous game in Java with Swing/Sockets.\nCreated by Abhay, Sameer, and James.", "About Skribbl", JOptionPane.INFORMATION_MESSAGE);
        });
        
        // Quit Program button
        buttons[3] = new JButton("Quit Skribbl");
        buttons[3].addActionListener((_) -> { exitProgram(); });
        
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

    // Asynchronous method which updates lobbiesListPanel after recieving data
    private void updateLobbiesListPanel(JPanel lobbiesListPanel, Dimension d) {
        (new SwingWorker<ArrayList<String[]>, Void>() {

            @Override
            protected ArrayList<String[]> doInBackground() throws Exception {
                LobbyListPack llp = new LobbyListPack();
                sendDataPackage(llp, MessageType.LOBBY_LIST);
                // System.out.println(llp.toJSON());
                String dataString = readJSONDataToString(MessageType.LOBBY_LIST);
                // System.out.println(dataString);
                llp = LobbyListPack.fromJSON(dataString);
                return llp.getLobbies();
            }

            protected void done() {
                // System.out.println("update lob list panel done hit");
                ArrayList<String[]> lobbies;
                try {
                    lobbies = get();
                } catch(Exception e) {
                    e.printStackTrace();
                    showMessageDialog("Failed to fetch list of lobbies. Try refreshing or restarting the app. The server may also be corrupt.", "Failed to Fetch Lobbies", JOptionPane.ERROR_MESSAGE);
                    lobbies = null;
                }

                lobbiesListPanel.removeAll();

                if (lobbies == null || lobbies.isEmpty()) {
                    JLabel label = new JLabel(lobbies == null ? "Failed to get lobbies" : "No lobbies currently");
                    label.setMinimumSize(new Dimension((int) d.getWidth(), (int) d.getHeight() / 8));
                    label.setFont(label.getFont().deriveFont(30.0f));
                    label.setAlignmentX(JLabel.CENTER_ALIGNMENT);
                    lobbiesListPanel.add(label);
                } else {
                    for (String[] lob : lobbies) {
                        JButton jb = new JButton();
                        jb.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 5));
                        jb.add(new JLabel(lob[2]));
                        jb.add(new JLabel(lob[0]));
                        jb.add(new JLabel("Players: " + lob[1]));
                        jb.setAlignmentX(JButton.CENTER_ALIGNMENT);
                        jb.setPreferredSize(new Dimension((int) (d.getWidth() / 3.5), (int) d.getHeight() / 7));
                        jb.addActionListener((_) -> {
                            joinPublicLobby(lob[0]);
                        });
                        lobbiesListPanel.add(jb);
                    }
                }

                System.out.println("lobbieslistpanel revaliding repainiting");

                lobbiesListPanel.revalidate();
                lobbiesListPanel.repaint();
            }

        }).execute();
    }
    
    private void leaveLobby() {
        isInLobby = false;
        (new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                // if(isInLobby) {
                    // isInLobby = false;
                    System.out.println("isInLobby value (leave): " + isInLobby);
                    LobbyLeavePack llp = new LobbyLeavePack();
                    System.out.println("Sending lobby leave pack (leave)");
                    sendDataPackage(llp, MessageType.LOBBY_LEAVE); 
                    System.out.println("Waiting to read lobby leave pack response (leave)");  
                    String dataString = readJSONDataToString(MessageType.LOBBY_LEAVE);
                    System.out.println(dataString);
                // }
                return null;
            }

            protected void done() {
                System.out.println("Hit leave() done");
                isHost = false;
                usersInGame = new ArrayList<String>();

                usersPanel = null;
                remove(currentPanel);
                currentPanel = getLobbiesPanel();
                add(currentPanel);
                revalidate();
                repaint();
            }

        }).execute();
    }

    private void joinPublicLobby(String lobbyName) {

        (new SwingWorker<LobbyJoinPack, Void>() {

            @Override
            protected LobbyJoinPack doInBackground() throws Exception {
                LobbyJoinPack ljp = new LobbyJoinPack(lobbyName);
                sendDataPackage(ljp, MessageType.LOBBY_JOIN);
                String json = readJSONDataToString(MessageType.LOBBY_JOIN);
                ljp = LobbyJoinPack.fromJSON(json);
                return (ljp.isSuccess() ? ljp : null);    
            }

            protected void done() {
                LobbyJoinPack ljp = null;
                try {
                    ljp = get();
                } catch(Exception e) {
                    showMessageDialog("Failed to send lobby join request", "Network Error", JOptionPane.ERROR_MESSAGE);
                }

                if(ljp != null) {
                    isInLobby = true;
                    isHost = false;
                    usersInGame.addAll(ljp.getPlayers());
                    remove(currentPanel);
                    currentPanel = getGamePanel(lobbyName, usersInGame, ljp.isStarted());
                    add(currentPanel);
                    revalidate();
                    repaint();
                }
            }

        }).execute();
    }
    
    // Create Lobby and display the game panel
    private void createLobby() {
        String[] lobbyArgs = promptForCreateLobbyArgs();
        if(lobbyArgs != null) {

            (new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    if(lobbyArgs.length == 1) {
                        LobbyInitPack lip = new LobbyInitPack(lobbyArgs[0]);
                        sendDataPackage(lip, MessageType.LOBBY_INIT);
                        String json = readJSONDataToString(MessageType.LOBBY_INIT);
                        lip = LobbyInitPack.fromJSON(json);
                        return lip.isSuccess();
                    } else if(lobbyArgs.length == 2) {
                        // TODO: Implement private lobbies
                        return false;
                    }
                    return false;            
                }

                protected void done() {
                    boolean createdLobby = false;
                    try {
                        createdLobby = get().booleanValue();
                    } catch(Exception e) {
                        showMessageDialog("Failed to send lobby creation request", "Network Error", JOptionPane.ERROR_MESSAGE);
                    }
                    if(createdLobby) {
                        isInLobby = true;
                        isHost = true;
                        usersInGame.add(username);
                        remove(currentPanel);
                        currentPanel = getGamePanel(lobbyArgs[0]);
                        add(currentPanel);
                        revalidate();
                        repaint();    
                    }
                }


            }).execute();
        }
    }
    
    // Prompt user for parameters to create a lobby
    private String[] promptForCreateLobbyArgs() {
        JTextField lobbyNameField = new JTextField("Lobby - " + (new java.util.Date()).toString());
        JCheckBox privateLobbyCheck = new JCheckBox();
        JTextField lobbyPasswordField = new JTextField("");
        
        lobbyPasswordField.setEnabled(false);
        privateLobbyCheck.addActionListener((_) -> {
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
        toRet.setLayout(new BorderLayout(10, 10));
        toRet.setPreferredSize(new Dimension(WIDTH, HEIGHT));

        JPanel westWrapper = new JPanel();
        westWrapper.setLayout(new BoxLayout(westWrapper, BoxLayout.Y_AXIS));

        usersPanel = new JPanel();
        usersPanel.setLayout(new BoxLayout(usersPanel, BoxLayout.Y_AXIS));

        for(String p: players) {
            usersPanel.add(new JLabel(p.equals(this.username) ? p + " (You)" : p));
        }
        
        JScrollPane usersScrollPane = new JScrollPane(usersPanel);
        usersScrollPane.setPreferredSize(new Dimension(WIDTH / 5, HEIGHT * 7 / 10));
        usersScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        usersScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JButton leaveButton = new JButton("Leave Lobby");
        leaveButton.setHorizontalAlignment(JLabel.LEFT);
        leaveButton.addActionListener((_) -> { leaveLobby(); });
        
        westWrapper.add(usersScrollPane);
        westWrapper.add(leaveButton);

        toRet.add(westWrapper, BorderLayout.WEST);

        return toRet;
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
        writer.flush();
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
            if(keepAliveThread != null && keepAliveThread.isAlive()) keepAliveThread.join();
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

            boolean startedHandler = false;

            while(runningProgram) {
                
                try {
                    if(!socket.isConnected() || socket.isClosed()) {
                        throw new IOException("Socket connection has been lost...");
                    }
                    long now = java.time.Instant.now().toEpochMilli();
                    if(now - lastPingRequest > 15000) {
                        sendDataPackage(new KeepAlivePack(), MessageType.KEEP_ALIVE);
                        lastPingRequest = now;
                    }

                } catch(IOException ioe) {
                    runningProgram = false;
                    showMessageDialog("Failed to send keep alive request, Program will end shortly...", "Network Error", JOptionPane.ERROR_MESSAGE);
                    exitProgram();
                }

                if(isInLobby && !startedHandler) {
                    System.out.println("starting handler...");
                    startedHandler = true;
                    (new SwingWorker<Void, String>() {
                        private Exception backgroundException = null;

                        @Override
                        protected Void doInBackground() {
                            try {
                                while (isInLobby && !isCancelled()) {
                                    System.out.println("doInbackground(): isInLobby value: " + isInLobby);
                                
                                    String packet = RawPacketHandler.readRawPacket(reader);
                                    System.out.println("FOSDJOSDKOFPSDF");
                                    publish(packet);
                                }
                            } catch (Exception e) {
                                System.out.println(e);
                                backgroundException = e;
                                cancel(true);
                            }
                            return null;
                        }

                        @Override
                        protected void process(List<String> packets) {
                            System.out.println("In process(): " + packets.size());
                            if(packets.size() > 0) System.out.println(packets.get(0));
                            for (String rawJson : packets) {
                                try {
                                    JSONObject obj = new JSONObject(rawJson);
                                    String type = obj.getString("type");

                                    JSONObject innerData = obj.getJSONObject("data");
                                    String innerDataString = innerData.toString();
                                    MessageType mt = MessageType.valueOf(type);

                                    if (mt == MessageType.LOBBY_JOIN) {
                                        LobbyJoinPack ljp = LobbyJoinPack.fromJSON(innerDataString);
                                        usersInGame = ljp.getPlayers();
                                        if (usersPanel != null) {
                                            usersPanel.removeAll();
                                            for (String p : usersInGame) {
                                                usersPanel.add(new JLabel(p.equals(username) ? p + " (You)" : p));
                                            }
                                            usersPanel.revalidate();
                                            usersPanel.repaint();
                                        }
                                    } else if (mt == MessageType.LOBBY_LEAVE) {
                                        LobbyLeavePack llp = LobbyLeavePack.fromJSON(innerDataString);
                                        usersInGame.remove(llp.getUsername());
                                        if (usersPanel != null) {
                                            usersPanel.removeAll();
                                            for (String p : usersInGame) {
                                                usersPanel.add(new JLabel(p.equals(username) ? p + " (You)" : p));
                                            }
                                            usersPanel.revalidate();
                                            usersPanel.repaint();
                                        }
                                    } else {
                                        System.out.println("Recieved MessageType: " + mt);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        @Override
                        protected void done() {
                            if (backgroundException != null) {
                                SwingUtilities.invokeLater(() -> {
                                    JOptionPane.showMessageDialog(null,
                                        "Network error occurred. You may have been disconnected.",
                                        "Network Error",
                                        JOptionPane.ERROR_MESSAGE);
                                    System.exit(0);
                                });
                            }
                        }

                    }).execute();

                }

                if(!isInLobby && startedHandler) {
                    startedHandler = false;
                }

            }
        }

    }

}
