package dev.abhay7.skribbl.client;

import java.awt.*;

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
        player = new Player("drawing");
        board = new Board(player);
    }

    public static void main(String... args) throws InterruptedException, FileNotFoundException {
        
        FlatLightLaf.registerCustomDefaultsSource("style");
        FlatLightLaf.setup();

        boolean gameGoing = true;
        Main main = new Main();
        JFrame frame = new JFrame("Skribbl 2");
        frame.getContentPane().setBackground(new Color(0, 0, 0, 0));
        int width = 1920;
        int height = 1080;
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

        JPanel drawingMenu = new JPanel(new BorderLayout());
        drawingMenu.setBackground(Color.WHITE);
        drawingMenu.setPreferredSize(new Dimension(
            (int) (width * 0.55), // 25% width of the frame
            (int) (height * 0.15) // 25% height of the frame
        ));
        

        board.setPreferredSize(new Dimension(
            (int) (width * 0.55),
            (int) (height * 0.45) 
        ));
        
        JLabel word = new JLabel("loading", SwingConstants.CENTER);
        word.setBackground(Color.WHITE);
        word.setOpaque(true);
        board.add(word, BorderLayout.NORTH);

        new Thread(() -> {
            String actualWord = getAWord(); // Blocking file IO
        
            // Now update the label on the EDT
            SwingUtilities.invokeLater(() -> word.setText(actualWord));
        }).start();

        leftPanel.add(board);
        leftPanel.add(Box.createVerticalStrut(10));
        leftPanel.add(drawingMenu);

        board.add(main);
        board.addMouseListener(main.player);
        board.addMouseMotionListener(main.player);

        JPanel chatArea = new JPanel();
        chatArea.setBackground(new Color(230, 230, 250)); // lavender
        chatArea.setPreferredSize(new Dimension(800, 800));
        
        JPanel sideContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 20));
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

}
