package net.andrews.mechtour.mapgui.gui;

import java.util.ArrayList;

import net.andrews.mechtour.mapgui.MapGuiHolder;

public abstract class MapGuiBase {

    private boolean shouldRenderAgain = true;
    protected ArrayList<InteractableElement> interactableElements = new ArrayList<>();

    public MapGuiBase() {

    }

    public void addInteractableElement(InteractableElement element) {
        this.interactableElements.add(element);
    }

    public void render(MapGuiHolder holder) {

        for (InteractableElement element : this.interactableElements) {
            element.render(holder);
            element.setReRenderFlag(false);
        }
    }

    public void onClose(MapGuiHolder holder) {

    }

    public void onOpen(MapGuiHolder holder) {
        this.setReRenderFlag(true);
    }

    public void onMousePosChange(MapGuiHolder holder, int newMouseX, int newMouseY, int oldMouseX, int oldMouseY) {

        for (InteractableElement element : this.interactableElements) {
            element.onMousePosChange(holder, newMouseX, newMouseY, oldMouseX, oldMouseY);
        }
      
    }
    public boolean shouldReRender(MapGuiHolder holder) {
        if (shouldRenderAgain) return true;

        for (InteractableElement element : this.interactableElements) {
            if (element.shouldReRender(holder)) {
                return true;
            }
        }

        return false;
    }

    public void setReRenderFlag(boolean value) {
        shouldRenderAgain = value;
    }


    public void onClick(boolean isInteractKey, MapGuiHolder holder) {

        for (InteractableElement element : this.interactableElements) {
            if (element.isMouseOver()) {
                element.onClick(isInteractKey, holder);
            }
        }

    }


    

}
