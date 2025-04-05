package dev.abhay7.skribbl.client;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.Border;

import java.util.*;
import java.io.*;

import java.lang.Thread;

public class Main extends JPanel {
    
    Player player; //eventually the server should decide what the player's initial state is
    Board board;  // change this to the server sends the starting board over to the client

    public Main() {
        player = new Player("drawing");
        board = new Board(player);
    }

    public static void main(String... args) throws InterruptedException, FileNotFoundException {
        System.out.println("Skribbl Client running!");
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

        JPanel gameRoom = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));

        Border border = BorderFactory.createLineBorder(Color.BLACK, 2);
        JPanel board = new JPanel(new BorderLayout());
        board.setBackground(Color.BLUE);
        board.setBorder(border);

        board.setPreferredSize(new Dimension(
            (int) (width * 0.45), // 25% width of the frame
            (int) (height * 0.45) // 25% height of the frame
        ));
        
        JLabel word = new JLabel("loading", SwingConstants.CENTER);
        word.setBackground(Color.WHITE);
        word.setBorder(border);
        word.setOpaque(true);
        board.add(word, BorderLayout.NORTH);

        new Thread(() -> {
            String actualWord = getAWord(); // Blocking file IO
        
            // Now update the label on the EDT
            SwingUtilities.invokeLater(() -> word.setText(actualWord));
        }).start();

        gameRoom.add(board);

        board.add(main);
        board.addMouseListener(main.player);
        board.addMouseMotionListener(main.player);
        
        
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
        g.fillRect(0,0,(int)(1920 * 0.45), (int)(1080 * 0.45));

        //super.paintComponent(g);
        board.paintComponent(g);        
    }

    public static String getAWord() {
        ArrayList<String> words = new ArrayList<String>();
        try {
            Scanner sc = new Scanner(new File("worddata.txt"));
            while (sc.hasNext()) {
                String word = sc.next();
                words.add(word.substring(0,word.length()-1));
            }
            sc.close();
        }
        catch (Exception e) {

        }

        return words.get((int) (Math.random() * words.size()));

    }

}
