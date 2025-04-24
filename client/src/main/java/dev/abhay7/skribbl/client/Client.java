package dev.abhay7.skribbl.client;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import dev.abhay7.skribbl.server.datapacks.LobbyJoinPack;

public class Client {

    public static final double ASPECT_RATIO = 16.0 / 9.0;
    public static final int HEIGHT = 720;
    public static final int WIDTH = (int) (HEIGHT * ASPECT_RATIO);
    public static final Dimension WINDOW_SIZE = new Dimension(WIDTH, HEIGHT);

    private JFrame frame;

    private String username;
    private String serverInet;
    private int serverPort;

    private volatile boolean isRunning;

    private volatile boolean isPlaying;
    private volatile boolean isHosting;

    private String lobbiesListTitle;
    private String inGameTitle;

    private ArrayList<String> currentPlayersList;

    private JPanel currentlyDisplayedPanel;
    private JPanel usersPanel;
    private JTextArea chatPanel;

    private Socket socket;
    private InputStream reader;
    private OutputStream writer;

    private NetworkHandler networkHandler;

    private static final byte MAX_DIALOG_COUNT = 5;
    private byte openDialogs;

    public Client(String... args) {
        if(args.length < 3 || args[0].isBlank() || args[1].isBlank() || args[2].equals("0")) {
            fatalError("Invalid Client Arguments", new IllegalArgumentException());
        }

        this.setRunning(true);

        this.username = args[0];
        this.serverInet = args[1];
        try { this.serverPort = Integer.parseInt(args[2]); } catch(Exception e) { fatalError("Invalid port value provided: " + args[2], e); }

        try {
            this.socket = new Socket(this.serverInet, this.serverPort);
            this.reader = socket.getInputStream();
            this.writer = socket.getOutputStream();

            networkHandler = new NetworkHandler(this, this.socket, this.reader, this.writer);
            networkHandler.verify(this.username);
        } catch(Exception e) {
            this.setRunning(false);
            String errMsg = "Failed to connect and verify with server";
            fatalError(errMsg, e);
            showMessageDialog(errMsg, "Failed to connect", JOptionPane.ERROR_MESSAGE);
        }

        if(this.isRunning()) {

            this.lobbiesListTitle = "skribbl.io (Java) - Lobbies - " + this.serverInet + ":" + this.serverPort;

            frame = new JFrame(this.lobbiesListTitle);
            frame.setSize(Client.WINDOW_SIZE);
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);

            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent we) {
                    exitProgram();
                }
            });

            networkHandler.start();

            currentlyDisplayedPanel = getLobbiesPanel();
            frame.add(currentlyDisplayedPanel);

            currentPlayersList = new ArrayList<String>();
            this.setPlaying(false);
            this.setHosting(false);
            frame.setVisible(true);
        
        } else {
            this.exitProgram();
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

    // Fetch the lobbies list    
    private void updateLobbiesListPanel(JPanel lobbiesListPanel, Dimension d) {
        (new SwingWorker<ArrayList<String[]>, Void>() {
    
            @Override
            protected ArrayList<String[]> doInBackground() throws Exception {
                return networkHandler.retrieveLobbyList().getLobbies();
            }

            protected void done() {
                ArrayList<String[]> lobbies;
                try {
                    lobbies = get();
                } catch(Exception e) {
                    showMessageDialog("Failed to fetch list of lobbies. Try refreshing or restarting the app. The server may also be corrupt: " + e, "Failed to Fetch Lobbies", JOptionPane.ERROR_MESSAGE);
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

                lobbiesListPanel.revalidate();
                lobbiesListPanel.repaint();
            }

        }).execute();
    }

    // Create a lobby - Prompt, network, and window updating.
    private void createLobby() {
        String[] lobbyArgs = promptForCreateLobbyArgs();
        this.inGameTitle = "skribbl.io (Java) - " + lobbyArgs[0]; 
        if(lobbyArgs != null) {
            (new SwingWorker<Boolean, Void>() {

                @Override
                protected Boolean doInBackground() throws Exception {
                    if(lobbyArgs.length == 1) {
                        return networkHandler.createPublicLobby(lobbyArgs[0]).isSuccess();
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
                    } catch (Exception e) {
                        showMessageDialog("Failed to create lobby: " + e, "Lobby Creation Error", JOptionPane.ERROR_MESSAGE);
                    }
                    if(createdLobby) {
                        isPlaying = true;
                        isHosting = true;
                        currentPlayersList.add(username);
                        frame.remove(currentlyDisplayedPanel);
                        currentlyDisplayedPanel = getGamePanel(lobbyArgs[0]);
                        frame.add(currentlyDisplayedPanel);
                        frame.setTitle(inGameTitle);
                        frame.revalidate();
                        frame.repaint();
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
        
        int result = JOptionPane.showConfirmDialog(this.frame, dialogPanel, "Create a new lobby", JOptionPane.OK_OPTION);
        
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
        return getGamePanel(lobName, this.currentPlayersList, false);
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

        JPanel eastWrapper = new JPanel();
        eastWrapper.setLayout(new BoxLayout(eastWrapper, BoxLayout.Y_AXIS));

        chatPanel = new JTextArea();
        chatPanel.setEditable(false);

        JScrollPane chatScrollPane = new JScrollPane(chatPanel);
        chatScrollPane.setPreferredSize(new Dimension((int) (WIDTH / 4.5), HEIGHT * 7 / 10));
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel sendMessagePanel = new JPanel();
        sendMessagePanel.setLayout(new FlowLayout());
        
        JTextField messageArea = new JTextField();
        messageArea.setPreferredSize(new Dimension(WIDTH / 6, HEIGHT * 1 / 20));
        messageArea.addActionListener((_) -> {
            sendMessage(messageArea, chatPanel);
        });

        JButton sendMessageButton = new JButton("Send");
        sendMessageButton.setPreferredSize(new Dimension(WIDTH / 15, HEIGHT * 1 / 20));
        sendMessageButton.addActionListener((_) -> {
            sendMessage(messageArea, chatPanel);
        });

        sendMessagePanel.add(messageArea);
        sendMessagePanel.add(sendMessageButton);

        eastWrapper.add(chatScrollPane);
        eastWrapper.add(sendMessagePanel);

        toRet.add(eastWrapper, BorderLayout.EAST);

        return toRet;
    }

    private void sendMessage(JTextField messageArea, JTextArea chatPanel) {
        if(messageArea.getText().isBlank()) return;
        (new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                networkHandler.sendMessage(messageArea.getText());
                return null;
            }

            protected void done() {
                boolean success = false;
                try {
                    get();
                    success = true;
                } catch(Exception e) {
                    chatPanel.setText(chatPanel.getText() + "\n" + "FAILED TO SEND MESSAGE");
                    success = false;
                }

                if(success) {
                    chatPanel.setText(chatPanel.getText() + "\n" + username + ": " + messageArea.getText());
                }
                messageArea.setText("");
            }

        }).execute();
    }
    
    // Leave a lobby
    private void leaveLobby() {
        this.isPlaying = false;
        this.isHosting = false;
        this.currentPlayersList = new ArrayList<String>();
        (new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                networkHandler.leaveLobby();
                return null;
            }

            protected void done() {
                try {
                    get();   
                } catch(Exception e) {
                    showMessageDialog("Error while leaving lobby: " + e, "Lobby Leave Error", JOptionPane.ERROR_MESSAGE);           
                }
                usersPanel.removeAll();
                usersPanel = null;
                chatPanel.removeAll();
                chatPanel = null;
                frame.remove(currentlyDisplayedPanel);
                currentlyDisplayedPanel = getLobbiesPanel();
                frame.add(currentlyDisplayedPanel);
                frame.setTitle(lobbiesListTitle);
                frame.revalidate();
                frame.repaint();
            }

        }).execute();
    }

    // Join a public lobby
    private void joinPublicLobby(String lobName) {
        this.currentPlayersList = new ArrayList<String>();
        this.inGameTitle = "skribbl.io (Java) - " + lobName; 
        (new SwingWorker<LobbyJoinPack, Void>() {

            @Override
            protected LobbyJoinPack doInBackground() throws Exception {
                return networkHandler.joinPublicLobby(lobName);
            }

            protected void done() {
                LobbyJoinPack ljp = null;
                try {
                    ljp = get();
                } catch (Exception e) {
                    showMessageDialog("Failed to join lobby: " + e, "Lobby Join Error", JOptionPane.ERROR_MESSAGE);
                }
                if(ljp != null && ljp.isSuccess()) {
                    isPlaying = true;
                    isHosting = false;
                    currentPlayersList.addAll(ljp.getPlayers());
                    frame.remove(currentlyDisplayedPanel);
                    currentlyDisplayedPanel = getGamePanel(lobName);
                    frame.add(currentlyDisplayedPanel);
                    frame.setTitle(inGameTitle);

                    // TODO: If ljp.isStarted(), do some other stuff

                    frame.revalidate();
                    frame.repaint();
                }
            }

        }).execute();
    }

    protected void updatePlayerList(ArrayList<String> usernames) {
        this.currentPlayersList = usernames;
        if (usersPanel != null) {
            SwingUtilities.invokeLater(() -> {
                usersPanel.removeAll();
                for (String p : this.currentPlayersList) {
                    usersPanel.add(new JLabel(p.equals(username) ? p + " (You)" : p));
                }
                usersPanel.revalidate();
                usersPanel.repaint();
            });
        }
    }

    protected void updateMessages(String message) {
        if (chatPanel != null) {
            SwingUtilities.invokeLater(() -> {
                chatPanel.setText(chatPanel.getText() + "\n" + message);
            });
        }
    }

    // Cleanly exit the program
    private synchronized void exitProgram() {
        try {
            this.setRunning(false);
            if(networkHandler != null && networkHandler.isAlive()) {
                networkHandler.disconnect();
                networkHandler.interrupt();
                networkHandler.join();
            }
            if(writer != null) writer.close();
            if(reader != null) reader.close();
            if(socket != null) socket.close();
        } catch(Exception ex) {
            System.err.println("Error while disconnecting from server: " + ex);
        }

        if(frame != null) {
            frame.setVisible(false);
            frame.dispose();
        }
        
        System.exit(0);
    }

    // Show Dialog Box Messages
    private void showMessageDialog(String msg, String titleMsg, int errorCode) {
        showMessageDialog(msg, titleMsg, errorCode, true);
    }
    private void showMessageDialog(String msg, String titleMsg, int errorCode, boolean invokeLater) {
        if(invokeLater) {
            SwingUtilities.invokeLater(() -> {
                if(openDialogs < MAX_DIALOG_COUNT) {
                    openDialogs++;
                    JOptionPane.showMessageDialog(this.frame, msg, titleMsg, errorCode);
                } else {
                    fatalError("Too many error dialogs called", null);
                }
                openDialogs--;
            });
        } else {
            if(openDialogs < MAX_DIALOG_COUNT) {
                openDialogs++;
                JOptionPane.showMessageDialog(this.frame, msg, titleMsg, errorCode);
            } else {
                fatalError("Too many error dialogs called", null);
            }
            openDialogs--;
        }
    }


    // If there is a fatal error (one that would completely impede program function), print the error and exit.
    private void fatalError(String message, Exception e) {
        System.err.println("FATAL Error: " + message);
        if(e != null) e.printStackTrace(System.err);
        System.exit(1);
    }

    // Basic getters and setters for boolean values
    public synchronized boolean isRunning() { return this.isRunning; }
    public synchronized void setRunning(boolean running) { this.isRunning = running; }
    
    public synchronized boolean isPlaying() { return this.isPlaying; }
    public synchronized void setPlaying(boolean playing) { this.isPlaying = playing; }
    
    public synchronized boolean isHosting() { return this.isHosting; }
    public synchronized void setHosting(boolean host) { this.isHosting = host; }

    protected synchronized ArrayList<String> getCurrentPlayersList() { return this.currentPlayersList; }
}