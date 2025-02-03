package net.andrews.sooncmp.slideshow.gui;

import java.util.List;

import com.mojang.datafixers.util.Pair;

import net.andrews.sooncmp.mapgui.MapText;
import net.andrews.sooncmp.slideshow.MapRenderer;
import net.andrews.sooncmp.slideshow.SlideshowGUI;

public class SimpleTextButton extends InteractableElement {
    private MapText text;
    private int x;
    private int y;
    private int width;
    private int height;
    private byte fillColor;
    private byte hoverColor;
    private byte textColor;
    private byte hoverTextColor;
    private boolean textHidden = false;

    SimpleTextButton(MapText text, byte fillColor, byte hoverColor, byte textColor, byte hoverTextColor) {

        this.text = text;
      
        this.fillColor = fillColor;
        this.hoverColor = hoverColor;
        this.textColor = textColor;
        this.hoverTextColor = hoverTextColor;
       
    }

    public void setDimensions(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.setInteractionBounds(x, y, x + width, y + height);
    }

    public void setTextHidden(boolean h) {
        this.textHidden = h;
    }

    @Override
    public void render(SlideshowGUI holder) {
        
        byte color = this.isMouseOver() ? hoverColor : fillColor;

        byte tcolor = this.isMouseOver() ? hoverTextColor : textColor;

        MapRenderer.fill(holder, x, y, width, height, color);

        if (!textHidden)
            MapRenderer.drawText(holder, text, x + width/2 - text.getWidth()/2, y + height/2 - text.getHeight() / 2, tcolor);


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
