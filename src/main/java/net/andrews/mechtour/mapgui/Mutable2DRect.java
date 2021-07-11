package net.andrews.mechtour.mapgui;


public class Mutable2DRect {
    private int minX;
    private int emaxX;
    private int minY;
    private int emaxY;

    public Mutable2DRect(int minX, int minY, int emaxX, int emaxY) {
        this.set(minX, minY, emaxX, emaxY);
    }

    public void set(int minX, int minY, int emaxX, int emaxY) {
        this.minX = minX;
        this.emaxX = emaxX;
        this.minY = minY;
        this.emaxY = emaxY;
    }

    public void includePos(int x, int y) {
        this.minX = Math.min(x, this.minX);
        this.minY = Math.min(y, this.minY);
        this.emaxX = Math.max(x + 1, this.emaxX);
        this.emaxY = Math.max(y + 1, this.emaxY);
    }
    public boolean includesPos(int x, int y) {
        return x > minX && x < emaxX && y > minY && y < emaxY;
    }
   
    public void setEMaxX(int maxX) {
        this.emaxX = maxX;
    }
    public void setEMaxY(int maxY) {
        this.emaxY = maxY;
    }
    public void setMinX(int minX) {
        this.minX = minX;
    }
    public void setMinY(int minY) {
        this.minY = minY;
    }
    public int getEMaxX() {
        return emaxX;
    }
    public int getEMaxY() {
        return emaxY;
    }
    public int getMinX() {
        return minX;
    }
    public int getMinY() {
        return minY;
    }
    public int getSize() {
        return (emaxX - minX) * (emaxY - minY);   
    }
    public int getWidth() {
        return emaxX - minX;
    }
    public int getHeight() {
        return emaxY - minY;
    }
}
