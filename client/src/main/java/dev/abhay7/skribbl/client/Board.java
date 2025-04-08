package dev.abhay7.skribbl.client;

import javax.swing.*;
import java.awt.*;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
//import java.io.Serializable;
import java.util.ArrayList;

public class Board extends JPanel {
    private Player currPlayer;

    private int prevMousex = 0;
    private int prevMousey = 0;


    Color color;
    
    //this will be updated to more than lines LATER - for now im keeping it simple
    private static ArrayList<Line> drawings;

    public Board(Player p) {
        drawings = new ArrayList<>();
        currPlayer = p;
        color = Color.black;
    }

    public void setCurrPlayer(Player player) {
        currPlayer = player;
        prevMousex = currPlayer.getMouseX();
        prevMousey = currPlayer.getMouseY();
    }

    public void changeColor(Color color) {
        this.color = color;
    }

    @Override
    protected void paintComponent(Graphics g) {
        int currX = currPlayer.getMouseX();
        int currY = currPlayer.getMouseY();


        if(currPlayer.getMouseDown()) {
            drawings.add(new Line(prevMousex, prevMousey-20, currX, currY-20, color));
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(3));
        for (Line l : Board.getLines()) {
            g2d.setColor(l.color);
            g2d.drawLine(l.startX, l.startY, l.endX, l.endY);
        }

        prevMousex = currX;
        prevMousey = currY;
    }

    public static ArrayList<Line> getLines() {
        return drawings;
    }
}
