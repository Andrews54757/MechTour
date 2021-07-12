package net.andrews.mechtour.mapgui;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.awt.Graphics2D;
import java.nio.file.Path;

import java.awt.RenderingHints;

import javax.imageio.ImageIO;

import net.andrews.mechtour.Configs;
import net.andrews.mechtour.mapgui.color.ColorMatcher;
import net.fabricmc.loader.api.FabricLoader;

public class BitMapImage {
    private int width = 0;
    private int height = 0;
    private int size = 0;

    private int alphaCutoff = 0;
    private BufferedImage toProcess;
    private byte[] mapImage = new byte[0];

    private int[] color = null;
    private boolean matchSlow = false;

    public BitMapImage(String image) {
       // image = image.replaceAll("\\/","");
        
        Path path = FabricLoader.getInstance().getConfigDir().resolve("mechtour/" + image);
        try {
            toProcess = ImageIO.read(new File(path.toString()));

        //    System.out.println("[MechTour] Loaded " + image);
        } catch (IOException e) {
            System.out.println("[MechTour] Could not read image " + image + " - " + e);
        }
    }
    public BitMapImage(BufferedImage image) {
        toProcess = image;
    }
    public BitMapImage scaledDimensions(int width, int height) {
        if (toProcess == null) return this;
            double aspectRatio = (double)toProcess.getWidth() / (double)toProcess.getHeight();

            if (width == -1) {
                width = (int)(aspectRatio * height);
            }

            if (height == -1) {
                height = (int)(width / aspectRatio);
            }

            if (width <= 0 || height <= 0) {

                System.out.println("Failed to load image " + toProcess.getWidth() +"," + toProcess.getHeight() + " - " + aspectRatio);
                this.toProcess = null;
                return this;
            }

            BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);

            
            Graphics2D g = newImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
            g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.drawImage(toProcess , 0, 0, width, height, null);
            g.dispose();
            toProcess = newImage;
            return this;
    }

    public BitMapImage setAlphaCutoff(int cutoff) {
        this.alphaCutoff = cutoff;
        return this;
    }

    public BitMapImage bake() {
        if (toProcess == null) return this;
        this.width = toProcess.getWidth();
        this.height = toProcess.getHeight();
        this.size = this.width * this.height;

        final byte[] pixels = ((DataBufferByte) toProcess.getRaster().getDataBuffer()).getData();
        final boolean hasAlphaChannel = toProcess.getAlphaRaster() != null;

        mapImage = new byte[this.size];

        int mapOffset = 0;
        int pixelLength = hasAlphaChannel ? 4 : 3;
        for (int pixel = 0; pixel + pixelLength - 1 < pixels.length; pixel += pixelLength) {
            int offset = 0;
            int a = 255;
            if (hasAlphaChannel) {
                a = pixels[pixel + (offset++)] & 0xFF;
            }
            int b = pixels[pixel + offset] & 0xFF;
            int g = pixels[pixel + offset + 1] & 0xFF;
            int r = pixels[pixel + offset + 2] & 0xFF;

            if (color != null) {
               
                double brightness = (double)(r + g + b) / 3.0 / 255.0;

                r = (int)((double)color[0] * brightness);
                g = (int)((double)color[1] * brightness);
                b = (int)((double)color[2] * brightness);
            }

            if (a <= alphaCutoff) {
                mapImage[mapOffset++] = 0;
            } else {
                mapImage[mapOffset++] = ColorMatcher.getBestColor(r, g, b, a, Configs.configs.fastColorMatch && !matchSlow);
            }
        }
        toProcess = null;
        color = null;
        matchSlow = false;
        return this;
    }

    public byte[] getMapImage() {
        return mapImage;
    }
    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
    public BitMapImage setColor(int i, int j, int k) {
        color = new int[] {i,j,k};
        return this;

    }
    public BitMapImage setMatchSlow(boolean matchSlow) {
        this.matchSlow = matchSlow;
        return this;
    }

}
