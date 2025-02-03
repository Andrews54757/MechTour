package net.andrews.sooncmp.slideshow.gui;

import java.util.ArrayList;
import java.util.List;

import com.mojang.datafixers.util.Pair;

import net.andrews.sooncmp.slideshow.SlideshowGUI;
import net.minecraft.server.network.ServerPlayerEntity;


public abstract class MapGuiBase {

    private boolean shouldRenderAgain = true;
    protected ArrayList<InteractableElement> interactableElements = new ArrayList<>();

    public MapGuiBase() {

    }

    public void addInteractableElement(InteractableElement element) {
        this.interactableElements.add(element);
    }

    public void render(SlideshowGUI holder) {

        for (InteractableElement element : this.interactableElements) {
            element.render(holder);
            element.setReRenderFlag(false);
        }
    }

    public void onClose(SlideshowGUI holder) {

    }

    public void onOpen(SlideshowGUI holder) {
        this.setReRenderFlag(true);
    }

    public void onMousePosChange(SlideshowGUI holder, List<Pair<Integer, Integer>> old_positions_looking_at,
            List<Pair<Integer, Integer>> positions_looking_at) {

        for (InteractableElement element : this.interactableElements) {
            element.onMousePosChange(holder, old_positions_looking_at, positions_looking_at);
        }

    }


    public void onClick(ServerPlayerEntity player, Pair<Integer, Integer> mousepos, boolean isInteractKey, SlideshowGUI holder) {

        for (InteractableElement element : this.interactableElements) {
            if (element.isMouseOnElement(mousepos.getFirst(), mousepos.getSecond())) {
                element.onClick(player, mousepos, isInteractKey, holder);
            }
        }

    }

    public boolean shouldReRender(SlideshowGUI holder) {
        if (shouldRenderAgain)
            return true;

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

    public void onScrollDown() {
        for (InteractableElement element : this.interactableElements) {
            element.onScrollDown();

        }
    }

    public void onScrollUp() {
        for (InteractableElement element : this.interactableElements) {
            element.onScrollUp();
        }
    }

    public boolean isScrollable() {
        return false;
    }

}
