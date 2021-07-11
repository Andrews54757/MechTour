package net.andrews.mechtour.mapgui.gui;

import net.andrews.mechtour.mapgui.IClickCallback;
import net.andrews.mechtour.mapgui.MapGuiHolder;
import net.andrews.mechtour.mapgui.Mutable2DRect;

public abstract class InteractableElement extends MapGuiBase {
    private Mutable2DRect interaction_bounds = new Mutable2DRect(0, 0, 0, 0);
    private boolean isMouseOver = false;
    private IClickCallback onClickCallback = null;

    public InteractableElement() {

    }

    public void setInteractionBounds(int minX, int minY, int emaxX, int emaxY) {
        interaction_bounds.set(minX, minY, emaxX, emaxY);
    }

    public void setClickCallback(IClickCallback callback) {
        onClickCallback = callback;
    }

    public void onClick(boolean isInteractKey, MapGuiHolder holder) {
        if (onClickCallback != null) {
            onClickCallback.call(isInteractKey, holder);
        }
        super.onClick(isInteractKey, holder);
    }

    public boolean isMouseOnElement(int x, int y) {
        return interaction_bounds.includesPos(x, y);
    }

    @Override
    public void onMousePosChange(MapGuiHolder holder, int newMouseX, int newMouseY, int oldMouseX, int oldMouseY) {
        if (isMouseOnElement(newMouseX, newMouseY)) {
            if (!isMouseOver) {
                isMouseOver = true;

                onMouseOver(holder, newMouseX, newMouseY);
            }
        } else {
            if (isMouseOver) {
                isMouseOver = false;

                onMouseOut(holder, newMouseX, newMouseY);
            }
        }
        super.onMousePosChange(holder, newMouseX, newMouseY, oldMouseX, oldMouseY);
        
    }

    public void onMouseEnter(MapGuiHolder holder, int mouseX, int mouseY) {

    }

    public void onMouseExit(MapGuiHolder holder, int mouseX, int mouseY) {

    }

    public void onMouseOver(MapGuiHolder holder, int mouseX, int mouseY) {

    }

    public void onMouseOut(MapGuiHolder holder, int mouseX, int mouseY) {

    }

    public boolean isMouseOver() {
        return isMouseOver;
    }


}
