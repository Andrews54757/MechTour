package net.andrews.sooncmp.slideshow.gui;

import java.util.List;

import com.mojang.datafixers.util.Pair;

import net.andrews.sooncmp.mapgui.color.MapColors;
import net.andrews.sooncmp.mapgui.gui.Resources;
import net.andrews.sooncmp.slideshow.SlideshowGUI;
import net.minecraft.server.network.ServerPlayerEntity;
import net.andrews.sooncmp.slideshow.MapRenderer;

public class MainMenuGUI extends MapGuiBase {



    private SimpleIconButton startSlideshow;

    public MainMenuGUI() {

        startSlideshow = new SimpleIconButton(Resources.slideshow_icon, (byte)(MapColors.SNOW + MapColors.BASE_COLOR), (byte)(MapColors.SNOW + MapColors.DARKER_COLOR));
       

        startSlideshow.setClickCallback((ServerPlayerEntity player, Pair<Integer, Integer> mousepos, boolean isInteract, SlideshowGUI holder) -> {
            holder.openGui(new SlideshowMenuGui());
        });

        addInteractableElement(startSlideshow);


     
    }

    @Override
    public void render(SlideshowGUI holder) {
      //  MapRenderer.fill(holder, (byte) 0);
       // banner = new BitMapImage("banner.png").scaledDimensions(-1, 100).setAlphaCutoff(200).bake();
        MapRenderer.fill(holder, (byte)(MapColors.SNOW + MapColors.BASE_COLOR));
        MapRenderer.drawImage(holder, Resources.welcome, holder.getPanelPixelWidth() / 2 - Resources.welcome.getWidth() / 2, holder.getPanelPixelHeight() / 2 - Resources.welcome.getHeight() / 2);
       // MapRenderer.drawImage(holder, Resources.banner,0, 0);
        int bWidth = 100;
        int bHeight = 80;
        startSlideshow.setDimensions(holder.getPanelPixelWidth() / 2 - bWidth / 2,
        holder.getPanelPixelHeight() - bHeight - 5, bWidth, bHeight);

        super.render(holder);
    }

    @Override
    public void onMousePosChange(SlideshowGUI holder, List<Pair<Integer, Integer>> old_positions_looking_at,
            List<Pair<Integer, Integer>> positions_looking_at) {

        super.onMousePosChange(holder, old_positions_looking_at, positions_looking_at);

    }

}