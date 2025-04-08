package dev.abhay7.skribbl.client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.border.Border;

import java.util.*;
import java.io.*;

import java.lang.Thread;

import com.formdev.flatlaf.*;

public class Main extends JPanel {
    
    Player player; //eventually the server should decide what the player's initial state is
    Board board;  // change this to the server sends the starting board over to the client

    public Main() {
        player = new Player("drawing", "myguy");
        board = new Board(player);
    }

    public static void main(String... args) throws InterruptedException, FileNotFoundException {
        
        //FlatIntelliJLaf.registerCustomDefaultsSource("style");
        FlatIntelliJLaf.setup();

        /*THE CLIENT SHOULD ADD MESSAGES ITSELF AND ADD A NEW STRING EVERYTIME IT GETS A CHAT MESSAGE
         *
         * GRAY OUT CHAT MESSAGES
         */
        ArrayList<String> textMessages = new ArrayList<String>();

        Border border = BorderFactory.createLineBorder(Color.black);

        boolean gameGoing = true;
        Main main = new Main();
        JFrame frame = new JFrame("Skribbl 2");
        frame.getContentPane().setBackground(new Color(0, 0, 0, 0));
        int width = 1280;
        int height = 720;
        frame.setSize(width, height);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel gameRoom = new JPanel(new BorderLayout());

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        JPanel board = new JPanel(new BorderLayout());
        board.setBackground(Color.WHITE);

        JPanel drawingMenu = new JPanel(new FlowLayout());
        drawingMenu.setBackground(Color.WHITE);
        drawingMenu.setPreferredSize(new Dimension(
            (int) (width * 0.65), // 25% width of the frame
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
        

        board.setPreferredSize(new Dimension(
            (int) (width * 0.65),
            (int) (height * 0.70) 
        ));
        
        JLabel word = new JLabel("loading", SwingConstants.CENTER);
        word.setBackground(Color.LIGHT_GRAY);
        word.setOpaque(true);
        board.add(word, BorderLayout.NORTH);

        board.setBorder(border);

        //helps deal with the EDT
        new Thread(() -> {
            String actualWord = getAWord();
        
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
            (int) (height * 0.90) // 25% height of the frame
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

        chatArea.add(chatBox);

        JTextArea chat = new JTextArea();
        chat.setEditable(false);
        chat.setLineWrap(false);
        chat.setWrapStyleWord(true);

        JScrollPane scrollChat = new JScrollPane(chat);

        chatBox.add(scrollChat, BorderLayout.CENTER);
        
        
        JPanel sideContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 20));
        sideContainer.add(leftPanel);
        sideContainer.setBackground(new Color(230, 230, 250));
        

        gameRoom.add(sideContainer, BorderLayout.WEST);
        gameRoom.add(chatArea, BorderLayout.CENTER);



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
        g.fillRect(0,0,(int)(1920 * 0.55), (int)(1080 * 0.55));

        //super.paintComponent(g);
        board.paintComponent(g);        
    }

    public static String getAWord() {
        ArrayList<String> words = new ArrayList<String>();
        try {
            Scanner sc = new Scanner(new File("client/src/main/java/dev/abhay7/skribbl/client/worddata.txt"));
            while (sc.hasNext()) {
                String word = sc.next();
                words.add(word.substring(0,word.length()-1));
            }
            sc.close();
        }
        catch (Exception e) {
            System.out.println(e);
        }

        return words.get((int) (Math.random() * words.size()));

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

}


