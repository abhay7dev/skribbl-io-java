package dev.abhay7.skribbl.client;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;

public class Board extends JPanel implements MouseListener {

    private NetworkHandler networkHandler;

    public Board(NetworkHandler networkHandler) {
        this.networkHandler = networkHandler;
    
        this.setFocusable(true);
        this.requestFocus();
    
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {    
    }
    
    private class Line {
        private int startX;
        private int startY;
    
        private int endX;
        private int endY;
    
        private Color color;
    
        public Line(int sX, int sY, int eX, int eY, Color clr) {
            startX = sX;
            startY = sY;
    
            endX = eX;
            endY = eY;
    
            color = clr;
        }
    
        private int getStartX() { return this.startX; }
        private int getStartY() { return this.startY; }
        private int getEndX() { return this.endX; }
        private int getEndY() { return this.endY; }
        private Color getColor() { return this.color; }
    
    }

}
