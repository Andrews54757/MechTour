package net.andrews.mechtour.mapgui;
import java.util.ArrayList;


public class MapRenderer {
    
    public static void clear(MapGuiHolder holder) {
        fill(holder, (byte)0);
    }

    public static void clear(MapGuiHolder holder, int x, int y, int width, int height) {
        fill(holder, x, y, width, height, (byte)0);
    }

    public static void fill(MapGuiHolder holder, byte color) {
        fill(holder, 0, 0, holder.getPanelPixelWidth(), holder.getPanelPixelHeight(), color);
    }

    public static void fill(MapGuiHolder holder, int x, int y, int width, int height, byte color) {
        for (int x2 = x; x2 < x + width; x2++) {
            for (int y2 = y; y2 < y + height; y2++) {
                holder.setPixel(x2, y2, color);
            }
        }
    }

    public static void drawText(MapGuiHolder holder, MapText text, int x, int y, byte color) {
        ArrayList<Integer> mask = text.getBitmask();

        for (int index : mask) {
            int mx = index % text.getWidth();
            int my = index / text.getWidth();

            holder.setPixel(x + mx, y + my, color);
        }
    }

    public static void drawImage(MapGuiHolder holder, BitMapImage image, int x, int y) {
        byte[] colors = image.getMapImage();

        for (int i = 0; i < colors.length; i++) {
            int mx = i % image.getWidth();
            int my = i / image.getWidth();

            if (colors[i] != 0)
                holder.setPixel(x + mx, y + my, colors[i]);
        }
    }
}
