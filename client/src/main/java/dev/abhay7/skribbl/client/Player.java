package dev.abhay7.skribbl.client;

import java.awt.event.*;

import javax.swing.*;

public class Player extends JPanel implements MouseMotionListener, MouseListener {
    String playerState;
    Board board;

    private int mouseX;
    private int mouseY;
    private boolean mouseDown;

    
    public Player(String playerState) {
        this.playerState = playerState;
        
        // Register MouseListener and MouseMotionListener
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        
        // Ensure component is focusable to receive mouse events
        this.setFocusable(true);
        this.requestFocus();
    }

    public void changePlayerState(String p) {
        playerState = p;
    }

    public String getPlayerState() {
        return playerState;
    }

    public int getMouseX() {
        System.out.println(mouseX + " " + mouseY);
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    public boolean getMouseDown() {
        return mouseDown;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mouseDown = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mouseDown = false;
    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }
}
