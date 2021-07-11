package net.andrews.mechtour.mapgui;


import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import java.awt.image.DataBufferByte;
import java.util.ArrayList;

public class MapText {
    
    private String text;
    private Font font;
    private ArrayList<Integer> bitmask = new ArrayList<>();
    private int width;
    private int height;
    

    public MapText(String text, Font font) {

        this.text = text;
        this.font = font;
        generateBitmask();
    }

    private void generateBitmask() {

        /*
           Because font metrics is based on a graphics context, we need to create
           a small, temporary image so we can ascertain the width and height
           of the final image
         */

        String [] lines = text.split("\n");
        int width = 0;
        int height = 0;

        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        height = 0;
        for (int i = 0; i < lines.length; i++) {
            width = Math.max(width, fm.stringWidth(lines[i]));
            height += fm.getHeight();
          //  if (i != 0) height += (int)((float)fm.getHeight() / 5);
        }

        g2d.dispose();
        img = new BufferedImage(width + 1, height, BufferedImage.TYPE_BYTE_GRAY);
        g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setFont(font);
        fm = g2d.getFontMetrics();
        g2d.setColor(Color.WHITE);
        int offsetY = fm.getAscent();
        int increment = fm.getHeight();
        for (int i = 0; i < lines.length; i++) {
            g2d.drawString(lines[i], 0, offsetY + increment * i);
        }
      
        g2d.dispose();
        
        byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        bitmask.clear();

        int index = 0;
        for (int i = 0; i < data.length; i++) {
            index++;
            if ((data[i] & 0xFF) != 0) {
                bitmask.add(index);
            }
        }

        this.width = img.getWidth();
        this.height = img.getHeight();
    }
    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
    public ArrayList<Integer> getBitmask() {
        return bitmask;
    }
}
