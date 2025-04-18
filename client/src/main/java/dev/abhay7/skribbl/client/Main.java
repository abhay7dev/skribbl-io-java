package dev.abhay7.skribbl.client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.border.Border;

import java.util.*;
import java.io.*;

import java.lang.Thread;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.formdev.flatlaf.*;
import java.net.*;

/*
 * PLANS:
 * Since Client is in a lobby in the server, it just needs to request data and the server will give it. Neat!
 *  - differentiate between the different types of data to know what to put where
 *      - client does its own thing THEN syncs up with server
 * 
 * Guessing - guesses should be handled by the DRAWING client, who knows what the word is.
 *  - the drawing client while putting in the chat also checks if its correct and sends that data back to everyone else
 * 
 * "Ghost" texter - When the client guesses right only people who already have a "won" tag or something get the messages
 * 
 * Scoring - scoring should also be handled by the drawing client, who can keep track of who guesses first, second, etc. Tally up points and distribute it at the end of the round
 *  
 * Mostly I'm thinking the logic should be handled by the drawing person just because they know the word data
 * 
 */

public class Main extends JPanel {
    
    Player player; //eventually the server should decide what the player's initial state is
    Board board;  // change this to the server sends the starting board over to the client

    static int width = 1280;
    static int height = 720;

    String PORT;

    static ArrayList<String> words = new ArrayList<String>();

    static ArrayList<Player> playerList = new ArrayList<>();

    public Main() {
        player = new Player("drawing", "myguy");
        board = new Board(player, (int) (1280 * 0.55), (int) (height * 0.70));

        //TEMP CODE - change code for fetching player
        playerList.add(new Player("chatting", "Jeffery"));
        playerList.add(new Player("chatting", "Lalalalala"));
        playerList.add(new Player("chatting", "Lebron James"));
    }

    public static void main(String... args) throws InterruptedException, FileNotFoundException, URISyntaxException, IOException {
        // System.out.println("Working Directory = " + System.getProperty("user.dir"));

        words = getWordList();
        Main main = new Main();
        
        //FlatIntelliJLaf.registerCustomDefaultsSource("style");
        FlatIntelliJLaf.setup();

        /*THE CLIENT SHOULD ADD MESSAGES ITSELF AND ADD A NEW STRING EVERYTIME IT GETS A CHAT MESSAGE
         *
         * GRAY OUT CHAT MESSAGES
         */
        ArrayList<String> textMessages = new ArrayList<String>();

        Border border = BorderFactory.createLineBorder(Color.black);

        JFrame serverList = new JFrame("Lobby List");

        JFrame portInput = new JFrame("SeverIP");
        portInput.setLayout(new FlowLayout());
        portInput.setVisible(true);
        portInput.setSize(200, 80);


        JTextField portInputBox = new JTextField(7);
        JButton pButton = new JButton("Confirm");
        pButton.addActionListener(e -> {
            main.PORT = portInputBox.getText();
            portInput.setVisible(false);
            serverList.setVisible(true);
        });

        portInput.add(portInputBox);
        portInput.add(pButton);

        String ip = "ip"; //how do we do this?
        Socket connection = new Socket(ip, Integer.parseInt(main.PORT));
        PrintWriter writer = new PrintWriter(connection.getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        boolean gameGoing = true;
        JFrame frame = new JFrame("Skribbl 2");
        frame.getContentPane().setBackground(new Color(0, 0, 0, 0));
        
        frame.setSize(width, height);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBackground(new Color(230,230,250));


        JPanel gameRoom = new JPanel(new BorderLayout());

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        JPanel board = new JPanel(new BorderLayout());
        board.setBackground(Color.WHITE);

        JPanel drawingMenu = new JPanel(new FlowLayout());
        drawingMenu.setBackground(Color.WHITE);
        drawingMenu.setPreferredSize(new Dimension(
            (int) (width * 0.55), // 25% width of the frame
            (int) (height * 0.15) // 25% height of the frame
        ));

        drawingMenu.add(getColorButton(Color.RED, main.board));
        drawingMenu.add(getColorButton(Color.BLUE, main.board));
        drawingMenu.add(getColorButton(Color.GREEN, main.board));
        drawingMenu.add(getColorButton(Color.ORANGE, main.board));
        drawingMenu.add(getColorButton(Color.PINK, main.board));
        drawingMenu.add(getColorButton(Color.BLACK, main.board));
        drawingMenu.add(getColorButton(Color.WHITE, main.board));
        drawingMenu.add(getColorButton(Color.YELLOW, main.board));
        drawingMenu.add(getColorButton(Color.MAGENTA, main.board));
        drawingMenu.add(getColorButton(Color.CYAN, main.board));

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Board.delLines();
            }
        });

        drawingMenu.add(clearButton);
        

        board.setPreferredSize(new Dimension(
            (int) (width * 0.55),
            (int) (height * 0.70) 
        ));
        
        JLabel word = new JLabel("loading", SwingConstants.CENTER);
        word.setBackground(Color.LIGHT_GRAY);
        word.setOpaque(true);
        board.add(word, BorderLayout.NORTH);

        board.setBorder(border);

        //helps deal with the EDT
        new Thread(() -> {
            String actualWord = getAWord(words);
        
            SwingUtilities.invokeLater(() -> word.setText(actualWord));
        }).start();

        leftPanel.add(board);
        leftPanel.add(Box.createVerticalStrut(10));
        leftPanel.add(drawingMenu);
        leftPanel.setBackground(new Color(230, 230, 250));

        board.add(main);
        board.addMouseListener(main.player);
        board.addMouseMotionListener(main.player);

        JPanel chatArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 100, 20));
        chatArea.setBackground(new Color(230, 230, 250)); // lavender
        chatArea.setPreferredSize(new Dimension(800, 800));

        JPanel chatBox = new JPanel(new BorderLayout());
        chatBox.setPreferredSize(new Dimension(
            (int) (width * 0.30), // 25% width of the frame
            (int) (height * 0.60) // 25% height of the frame
        ));
        chatBox.setBorder(border);
        chatBox.setBackground(Color.WHITE);

        JLabel chatTitle = new JLabel("Chat", SwingConstants.CENTER);
        chatTitle.setBackground(Color.LIGHT_GRAY);
        chatTitle.setOpaque(true);
        chatBox.add(chatTitle, BorderLayout.NORTH);

        JTextField cField = new JTextField(10);
        JButton cButton = new JButton("Send");
        cButton.addActionListener(e -> {
            String in = cField.getText();
            if (!in.equals("")) textMessages.add(main.player.getName() + ": " + in);
            cField.setText("");
        });

        JPanel chatSend = new JPanel(new FlowLayout());

        chatSend.add(cField);
        chatSend.add(cButton);

        chatBox.add(chatSend, BorderLayout.SOUTH);

        JTextArea chat = new JTextArea();
        chat.setEditable(false);
        chat.setLineWrap(false);
        chat.setWrapStyleWord(true);

        JScrollPane scrollChat = new JScrollPane(chat);

        chatBox.add(scrollChat, BorderLayout.CENTER);
        
        chatArea.add(chatBox);

        JPanel j = new JPanel();
        j.setLayout(new BoxLayout(j, BoxLayout.Y_AXIS));
        j.setPreferredSize(new Dimension(200,200));
        j.setBackground(Color.WHITE);
        j.setBorder(border);
        j.add(new JLabel("PLAYER LIST"));
        for (Player p : playerList) {
            j.add(new JLabel(p.playerName));

            chatArea.add(j);
        }
        
        JPanel sideContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 20));
        sideContainer.add(leftPanel);
        sideContainer.setBackground(new Color(230, 230, 250));
        

        gameRoom.add(sideContainer, BorderLayout.WEST);
        gameRoom.add(chatArea, BorderLayout.CENTER);

        
        serverList.setVisible(false);
        serverList.setSize(width, height);
        serverList.getContentPane().setBackground(new Color(230, 230, 250));

        JLabel sTitle = new JLabel("Server List", SwingConstants.CENTER);
        sTitle.setFont(new Font("SansSerif", Font.BOLD, 32));

        JPanel serverListPanel = new JPanel();
        serverListPanel.setLayout(new BoxLayout(serverListPanel, BoxLayout.Y_AXIS));
        serverListPanel.setBackground(new Color(230, 230, 250));

        //create an arrayLIST FOR THE PANELS REFRESH IT

        for (int i = 1; i <= 5; i++) {
            serverListPanel.add(createServerM("Lobby " + i, "public", frame, serverList));
        }

        JScrollPane scrollPane = new JScrollPane(serverListPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);



        serverList.add(sTitle, BorderLayout.NORTH);
        serverList.add(scrollPane);
        

        frame.setVisible(false);
        frame.setResizable(true);
        frame.add(gameRoom);



        while(gameGoing) {
            //Cient does its own logic - sends to server


            //Clients gets relayed info from server - updates its own variables


            //Client displays the results
            chat.setText("");
            for (String s : textMessages) {
                chat.append(s + "\n");
            }

            SwingUtilities.invokeLater(() -> main.repaint());

            

            Thread.sleep(10);
        }
        
        

    }

    @Override
    public void paintComponent(Graphics g) {

        g.setColor(Color.WHITE);
        g.fillRect(0,0,(int)(1280 * 0.55), (int)(720 * 0.55));

        //super.paintComponent(g);
        board.paintComponent(g);
        g.drawImage(board.getDrawing(), 0, 0, null);        
    }

    public static String getAWord(ArrayList<String> words) {
        return words.get((int) (Math.random() * words.size()));

    }

    public static ArrayList<String> getWordList() throws URISyntaxException {
        ArrayList<String> words = new ArrayList<>();
        try {
            // Create URL object
            URL url = new URI("https://gist.githubusercontent.com/mvark/9e0682c62d75625441f6ded366245203/raw/aec8a476a210db88086c50d3735507510ea295f2/Skribbl-words.csv").toURL();
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;

            reader.readLine();

            // Read each line in the CSV file
            while ((line = reader.readLine()) != null) {
                words.add(line.split(",")[0]);  // Add each word to the list
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return words;
    }

    public static JButton getColorButton(Color c, Board b) {
            JButton but = new JButton();
            but.setBackground(c);

            but.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    b.changeColor(c);
                }
            });

            but.setPreferredSize(new Dimension(20,20));

            return but;
    }

    public static JPanel createServerM(String name, String sType, JFrame cFrame, JFrame jFrame) {
        JPanel sEntry = new JPanel(new BorderLayout());
        sEntry.setMaximumSize(new Dimension(1000, 60));
        sEntry.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JLabel nameLabel = new JLabel(name);
        JButton joinButton = new JButton("Join Server");

        joinButton.addActionListener(e -> {
            boolean in = true;

            if (sType.equals("protected")) {
                in = false;
                JPasswordField passwordField = new JPasswordField();
                int option = JOptionPane.showConfirmDialog(
                    sEntry, 
                    passwordField, 
                    "Enter Password", 
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
                );

                if (option == JOptionPane.OK_OPTION) {
                    //VALIDATE PASSWORD. HOW???? IDK
                    // I have an idea. Obviously, the client needs to receive data.
                    // When it receives a game packet that it knows it needs to interpret as encrypted data,
                    // it wants to decrypt it. With AES GCM, that decryptiojn only suceedes if the key is correctg
                    // Thus, when we have an inpersonating server (sending garbage data to the client), we fail to decrypt it and pop up a warning
                    // inidicating that EITHER the password is incorrect or the server isn't who they claim to be
                    // In either case, the symptoms are identical--and indeed, the sickness is, too
                    // Thus, wehnever a client accepts the connection, the server sends some initial packet to the client
                    // Which i believe abhay has already implemented
                    // The client is a state machine -- it waits for this packet before any more work can be done
                    // If it fails to decyrpt this initial packet, then boom, either the password is wrong or fake server.
                    in = true;
                }
            }

            if (in) {
                //server joining code
                cFrame.setVisible(true);
                jFrame.setVisible(false);
            }
            
        });

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.add(nameLabel);

        sEntry.add(textPanel, BorderLayout.CENTER);
        sEntry.add(joinButton, BorderLayout.EAST);

        return sEntry;
    }

}


