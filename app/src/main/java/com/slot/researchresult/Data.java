package com.slot.researchresult;

import java.io.Serializable;

public class Data implements Serializable {
    int x,y,r,g,b;

    public int getX() {
        return x;
    }

    public Data() {
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public int getG() {
        return g;
    }

    public void setG(int g) {
        this.g = g;
    }

    public int getB() {
        return b;
    }

    public void setB(int b) {
        this.b = b;
    }

    public int getR() {
        return r;
    }

    public void setR(int r) {
        this.r = r;
    }

    public Data(int x, int y, int r, int g, int b) {
        this.x = x;
        this.y = y;
        this.r = r;
        this.g = g;
        this.b = b;
    }
    public void setY(int y) {
        this.y = y;
    }

}
