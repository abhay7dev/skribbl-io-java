package dev.abhay7.skribbl.client;

import java.awt.*;

public class Line{
    int startX;
    int startY;

    int endX;
    int endY;

    Color color;

    public Line(int sX, int sY, int eX, int eY, Color clr) {
        startX = sX;
        startY = sY;

        endX = eX;
        endY = eY;

        color = clr;
    }

}
