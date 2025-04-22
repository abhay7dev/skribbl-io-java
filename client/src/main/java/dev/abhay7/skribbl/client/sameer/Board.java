package dev.abhay7.skribbl.client.sameer;

import javax.swing.*;

import java.awt.*;
import java.awt.image.BufferedImage;
// import java.io.FileOutputStream;
// import java.io.ObjectOutputStream;
//import java.io.Serializable;
import java.util.ArrayList;

public class Board extends JPanel {
    private Player currPlayer;

    private int prevMousex = 0;
    private int prevMousey = 0;

    BufferedImage im;

    Color color;
    
    //this will be updated to more than lines LATER - for now im keeping it simple
    private static ArrayList<Line> drawings;

    public Board(Player p, int w, int h) {
        drawings = new ArrayList<>();
        currPlayer = p;
        color = Color.black;
        im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
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

        
        Graphics2D g2d = im.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, (int)(1280 * 0.55), (int)(720 * 0.70));
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

    public static void delLines() {
        drawings.clear();
    }

    public BufferedImage getDrawing() {
        return im;
    }
}
