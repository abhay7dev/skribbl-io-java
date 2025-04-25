package dev.abhay7.skribbl.client;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

public class Board extends JPanel {

    private Color currentColor;
    private int size;

    private Client client;
    private Canvas canvas;

    private boolean isDrawing;

    public Board(Client cl, int WIDTH, int HEIGHT) {
        this.client = cl;
        
        currentColor = Color.BLACK;
        this.size = 10;

        isDrawing = true;

        canvas = new Canvas((int) (WIDTH * 0.9), (int) (HEIGHT * 0.7));

        JPanel drawingMenu = new JPanel(new FlowLayout());
        drawingMenu.setPreferredSize(new Dimension((int) (WIDTH * 0.9), (int) (HEIGHT * 0.2)));

        drawingMenu.add(getColorButton(Color.RED));
        drawingMenu.add(getColorButton(Color.BLUE));
        drawingMenu.add(getColorButton(Color.GREEN));
        drawingMenu.add(getColorButton(Color.ORANGE));
        drawingMenu.add(getColorButton(Color.PINK));
        drawingMenu.add(getColorButton(Color.BLACK));
        drawingMenu.add(getColorButton(Color.WHITE));
        drawingMenu.add(getColorButton(Color.YELLOW));
        drawingMenu.add(getColorButton(Color.MAGENTA));
        drawingMenu.add(getColorButton(Color.CYAN));

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.delLines();
            }
        });

        drawingMenu.add(clearButton);

        canvas.setBorder(BorderFactory.createLineBorder(Color.BLACK, 10));

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        this.add(canvas);
        this.add(drawingMenu);
    }

    public boolean isDrawing() { return this.isDrawing; }
    public void setDrawing(boolean drawing) { this.isDrawing = drawing; }
    public Canvas getCanvas() { return this.canvas; }

    private JButton getColorButton(Color c) {
        JButton but = new JButton();
        but.setBackground(c);

        but.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                currentColor = c;
            }
        });

        but.setPreferredSize(new Dimension(40, 40));

        return but;
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

    protected class Canvas extends JPanel {

        private int prevMouseX;
        private int prevMouseY;

        private BufferedImage image;

        private ArrayList<Line> drawings;
        private PlayerMouseListener pml;

        private Canvas(int WIDTH, int HEIGHT) {
            // this.WIDTH = WIDTH;
            // this.HEIGHT = HEIGHT;
            this.setSize(WIDTH, HEIGHT);
            this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
            
            drawings = new ArrayList<>();
            image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            
            pml = new PlayerMouseListener(this);
            this.addMouseListener(pml);
            this.addMouseMotionListener(pml);

            // prevMouseX = pml.getMouseX();
            // prevMouseY = pml.getMouseY();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Lazy image creation once actual dimensions are known
            if (image == null || image.getWidth() != getWidth() || image.getHeight() != getHeight()) {
                image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = image.createGraphics();
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }

            if(isDrawing) {
                int currX = pml.getMouseX();
                int currY = pml.getMouseY();

                if (pml.isMouseDown()) {
                    drawings.add(new Line(prevMouseX, prevMouseY, currX, currY, currentColor));
                }

                Graphics2D g2d = image.createGraphics();
                g2d.setStroke(new BasicStroke(size));

                for (Line l : drawings) {
                    g2d.setColor(l.getColor());
                    g2d.drawLine(l.getStartX(), l.getStartY(), l.getEndX(), l.getEndY());
                }

                g2d.dispose();
                g.drawImage(image, 0, 0, null);

                prevMouseX = currX;
                prevMouseY = currY;
            } else {
                g.drawImage(image, 0, 0, null);
            }
        }

        public ArrayList<Line> getLines() {
            return drawings;
        }

        public void delLines() {
            drawings.clear();


            Graphics2D g2d = image.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
            g2d.dispose();
        
            this.repaint();
        }

        private void setPrevMouse(int x, int y) {
            prevMouseX = x;
            prevMouseY = y;
        }

        public BufferedImage getDrawing() {
            return image;
        }

        public void setImage(BufferedImage img) {
            if (img != null) {
                this.image = img;
                drawings.clear();
                this.repaint();
            }
        }

    }

    private class PlayerMouseListener implements MouseMotionListener, MouseListener {
        
        private int mouseX;
        private int mouseY;
        private boolean isMouseDown;

        private Canvas canvas;

        private PlayerMouseListener(Canvas canvas) { this.canvas = canvas; }
        
        public int getMouseX() { return mouseX; }
        public int getMouseY() { return mouseY; }
        public boolean isMouseDown() { return isMouseDown; }

        @Override
        public void mouseDragged(MouseEvent e) {
            mouseX = e.getX();
            mouseY = e.getY();
            ((JComponent) e.getSource()).repaint();
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            mouseX = e.getX();
            mouseY = e.getY();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            isMouseDown = true;
            mouseX = e.getX();
            mouseY = e.getY();
            canvas.setPrevMouse(mouseX, mouseY);
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            isMouseDown = false;
            e.consume();
            try {
                client.getNetworkHandler().sendBoard(canvas.getDrawing());
            } catch(Exception er) {
                System.out.println(er);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {}
        @Override
        public void mouseEntered(MouseEvent e) {}
        @Override
        public void mouseClicked(MouseEvent e) {}

    }

}
