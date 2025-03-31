package dev.abhay7.skribbl.client;

import java.awt.event.*;

public class Player implements MouseMotionListener, MouseListener {
    String playerState;
    Board board;

    private int mouseX;
    private int mouseY;
    private boolean mouseDown;

    public Player(String playerState) {
        this.playerState = playerState;
    }

    public void changePlayerState(String p) {
        playerState = p;
    }

    public String getPlayerState() {
        return playerState;
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    public boolean getMouseDown() {
        return mouseDown;
    }

    public void mouseDragged(MouseEvent e) {

    }

    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    public void mousePressed(MouseEvent e) {
        mouseDown = true;
    }

    public void mouseReleased(MouseEvent e) {
        mouseDown = false;
    }

    public void mouseExited(MouseEvent e) {

    }

    public void mouseEntered(MouseEvent e) {

    }

    public void mouseClicked(MouseEvent e) {

    }
}
