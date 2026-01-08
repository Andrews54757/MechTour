package net.andrews.sooncmp.slideshow.gui;

import net.andrews.sooncmp.slideshow.IClickCallback;

import java.util.List;

import com.mojang.datafixers.util.Pair;

import net.andrews.sooncmp.mapgui.Mutable2DRect;
import net.andrews.sooncmp.slideshow.SlideshowGUI;
import net.minecraft.server.level.ServerPlayer;

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

    public void onClick(ServerPlayer player,Pair<Integer, Integer> mousepos, boolean isInteractKey, SlideshowGUI holder) {
        if (onClickCallback != null) {
            onClickCallback.call(player, mousepos, isInteractKey, holder);
        }
        super.onClick(player, mousepos, isInteractKey, holder);
    }

    public boolean isMouseOnElement(int x, int y) {
        return interaction_bounds.includesPos(x, y);
    }

    @Override
    public void onMousePosChange(SlideshowGUI holder, List<Pair<Integer, Integer>> old_positions_looking_at,
            List<Pair<Integer, Integer>> positions_looking_at) {

        
        if (positions_looking_at.stream().anyMatch(p -> isMouseOnElement(p.getFirst(), p.getSecond()))) {
            if (!isMouseOver) {
                isMouseOver = true;

                onMouseOver(holder, positions_looking_at);
            }
        } else {
            if (isMouseOver) {
                isMouseOver = false;

                onMouseOut(holder, positions_looking_at);
            }
        }
        super.onMousePosChange(holder, old_positions_looking_at, positions_looking_at);
        
    }

    public void onMouseOver(SlideshowGUI holder, List<Pair<Integer, Integer>> positions_looking_at) {

    }

    public void onMouseOut(SlideshowGUI holder, List<Pair<Integer, Integer>> positions_looking_at) {

    }

    public boolean isMouseOver() {
        return isMouseOver;
    }


}
