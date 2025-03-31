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

    public static void main(String... args) {
        System.out.println("Skribbl Client running!");
        boolean gameGoing = true;
        Main main = new Main();

        while(gameGoing) {
            //Cient does its own logic - sends to server


            //Clients gets relayed info from server - updates its own variables


            //Client displays the results
            main.repaint();
        }
        
        

    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        board.paintComponent(g);
    }

}
