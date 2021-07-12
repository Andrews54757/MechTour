package net.andrews.mechtour.mapgui.gui;

import net.andrews.mechtour.mapgui.BitMapImage;
import net.andrews.mechtour.mapgui.MapGuiHolder;
import net.andrews.mechtour.mapgui.MapRenderer;
import net.andrews.mechtour.mapgui.MapText;

public class SimpleTextIconButton extends InteractableElement {
    private BitMapImage image;
    private MapText text;
    private int x;
    private int y;
    private int width;
    private int height;
    private byte fillColor;
    private byte hoverColor;
    private byte textColor;
    private byte hoverTextColor;

    SimpleTextIconButton(BitMapImage image, MapText text, byte fillColor, byte hoverColor, byte textColor, byte hoverTextColor) {

        this.image = image;
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

    @Override
    public void render(MapGuiHolder holder) {
        
        byte color = this.isMouseOver() ? hoverColor : fillColor;

        byte tcolor = this.isMouseOver() ? hoverTextColor : textColor;

        MapRenderer.fill(holder, x, y, width, height, color);

        MapRenderer.drawText(holder, text, x + width/2 - text.getWidth()/2, y + height * 3/4 - text.getHeight() / 2, tcolor);
        MapRenderer.drawImage(holder, image, x + width/2 - image.getWidth()/2, y + height*1/3 - image.getHeight()/2);


    }

    @Override
    public void onMouseOver(MapGuiHolder holder, int mouseX, int mouseY) {
        
       this.setReRenderFlag(true);
    }

    @Override
    public void onMouseOut(MapGuiHolder holder, int mouseX, int mouseY) {
        this.setReRenderFlag(true);
    }
}
