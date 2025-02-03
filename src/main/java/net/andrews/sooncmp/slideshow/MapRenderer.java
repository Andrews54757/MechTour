package net.andrews.sooncmp.slideshow;

import java.util.ArrayList;

import net.andrews.sooncmp.mapgui.BitMapImage;
import net.andrews.sooncmp.mapgui.MapText;

public class MapRenderer {

    public static void clear(SlideshowGUI holder) {
        fill(holder, (byte) 0);
    }

    public static void clear(SlideshowGUI holder, int x, int y, int width, int height) {
        fill(holder, x, y, width, height, (byte) 0);
    }

    public static void fill(SlideshowGUI holder, byte color) {
        fill(holder, 0, 0, holder.getPanelPixelWidth(), holder.getPanelPixelHeight(), color);
    }

    public static void fill(SlideshowGUI holder, int x, int y, int width, int height, byte color) {
        for (int x2 = x; x2 < x + width; x2++) {
            for (int y2 = y; y2 < y + height; y2++) {
                holder.setPixel(x2, y2, color);
            }
        }
    }

    public static void drawText(SlideshowGUI holder, MapText text, int x, int y, byte color) {
        ArrayList<Integer> mask = text.getBitmask();

        for (int index : mask) {
            int mx = index % text.getWidth();
            int my = index / text.getWidth();

            holder.setPixel(x + mx, y + my, color);
        }
    }

    public static void drawImage(SlideshowGUI holder, BitMapImage image, int x, int y) {
        byte[] colors = image.getMapImage();
        int width = image.getWidth();
        int height = image.getHeight();
        for (int yi = 0; yi < height; yi++) {
            for (int xi = 0; xi < width; xi++) {
                if (colors[xi + yi * width] != 0) {
                    holder.setPixel(x + xi, y + yi, colors[xi + yi * width]);
                }
            }
        }

    }

    public static void drawImageAll(SlideshowGUI holder, BitMapImage image, int x, int y) {
        byte[] colors = image.getMapImage();
        int width = image.getWidth();
        int height = image.getHeight();
        for (int yi = 0; yi < height; yi++) {
            for (int xi = 0; xi < width; xi++) {
                holder.setPixel(x + xi, y + yi, colors[xi + yi * width]);
            }
        }
    }
}
