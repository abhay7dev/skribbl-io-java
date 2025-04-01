package dev.abhay7.skribbl.client;

import java.awt.*;

import javax.swing.*;

public class Main extends JPanel {
    
    Player player; //eventually the server should decide what the player's initial state is
    Board board;  // change this to the server sends the starting board over to the client

    public Main() {
        player = new Player("drawing");
        board = new Board(player);
    }

    public static void main(String... args) throws InterruptedException {
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

        frame.setResizable(true);
        frame.add(main);
        frame.addMouseListener(main.player);

        while(gameGoing) {
            //Cient does its own logic - sends to server


            //Clients gets relayed info from server - updates its own variables


            //Client displays the results
            main.repaint();

            Thread.sleep(10);
        }
        
        

    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        board.repaint();
    }

}
