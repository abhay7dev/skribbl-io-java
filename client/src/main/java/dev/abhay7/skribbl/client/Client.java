package dev.abhay7.skribbl.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

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

    public Client(String[] args) {
        if(args.length < 3 || args[0].isBlank() || args[1].isBlank() || args[2].equals("0")) {
            System.err.println("Invalid client arguments");
            System.exit(1);
        }

        this.username = args[0];
        this.serverInet = args[1];
        try {
            this.serverPort = Integer.parseInt(args[2]);
            socket = new Socket(this.serverInet, this.serverPort);
            reader = socket.getInputStream();
            writer = socket.getOutputStream();

            String receivedJsonString = RawPacketHandler.readRawPacket(reader);

            JSONObject receivedJsonObject = new JSONObject(receivedJsonString);
            String type = receivedJsonObject.getString("type");
            
            if(MessageType.valueOf(type) != MessageType.CLIENT_VERIFICATION) {
                throw new Exception("Invalid MessageType in request");
            }

            String innerDataString = receivedJsonObject.getJSONObject("data").toString();
            ClientVerificationPack responseVerification = ClientVerificationPack.fromJSON(innerDataString);
            responseVerification = new ClientVerificationPack(responseVerification.getVerificationString() + "_VERIFIEDCONNECTION", this.username);
            sendDataPackage(responseVerification, MessageType.CLIENT_VERIFICATION);

        } catch(Exception e) {
            System.err.println(e);
            JOptionPane.showMessageDialog(null, "Failed to connect to server. Your username may have been taken or your internet may be down.", "Network Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        this.setTitle("skribbl.io (Java) - Lobbies - " + this.serverInet + ":" + this.serverPort);
        this.setSize(Client.WINDOW_SIZE);
        this.setMinimumSize(Client.WINDOW_SIZE);
        this.setMaximumSize(Client.WINDOW_SIZE);
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    writer.close();
                    reader.close();
                    socket.close();
                    if(networkThread.isAlive()) networkThread.join();
                } catch(Exception ex) {
                    System.out.println("Error while disconnecting from server");
                }
                e.getWindow().dispose();
            }
        });

        this.add(getLobbiesPanel());

        this.setVisible(true);
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

        

        return toRet;
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

    
}
