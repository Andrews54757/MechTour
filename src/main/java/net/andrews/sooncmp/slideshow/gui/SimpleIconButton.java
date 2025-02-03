package net.andrews.sooncmp.slideshow.gui;

import java.util.List;

import com.mojang.datafixers.util.Pair;

import net.andrews.sooncmp.mapgui.BitMapImage;
import net.andrews.sooncmp.slideshow.MapRenderer;
import net.andrews.sooncmp.slideshow.SlideshowGUI;

public class SimpleIconButton extends InteractableElement {
    private BitMapImage image;
    private int x;
    private int y;
    private int width;
    private int height;
    private byte fillColor;
    private byte hoverColor;

    SimpleIconButton(BitMapImage image, byte fillColor, byte hoverColor) {

        this.image = image;
      
        this.fillColor = fillColor;
        this.hoverColor = hoverColor;
       
    }

    public void setDimensions(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.setInteractionBounds(x, y, x + width, y + height);
    }

    @Override
    public void render(SlideshowGUI holder) {
        
        byte color = this.isMouseOver() ? hoverColor : fillColor;


        MapRenderer.fill(holder, x, y, width, height, color);

        MapRenderer.drawImage(holder, image, x + width/2 - image.getWidth()/2, y + height/2 - image.getHeight()/2);
    }

    @Override
    public void onMouseOver(SlideshowGUI holder, List<Pair<Integer, Integer>> positions_looking_at) {
        
       this.setReRenderFlag(true);
    }

    @Override
    public void onMouseOut(SlideshowGUI holder, List<Pair<Integer, Integer>> positions_looking_at) {
        this.setReRenderFlag(true);
    }
}
