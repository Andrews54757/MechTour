package net.andrews.sooncmp.slideshow.gui;

import net.andrews.sooncmp.SoonCMPMod;
import net.andrews.sooncmp.Utils;
import net.andrews.sooncmp.mapgui.BitMapImage;
import net.andrews.sooncmp.slideshow.MapRenderer;
import net.andrews.sooncmp.slideshow.SlideshowGUI;
import net.andrews.sooncmp.slideshow.SlideshowManager;
import net.andrews.sooncmp.mapgui.MapText;
import net.andrews.sooncmp.mapgui.color.MapColors;
import net.andrews.sooncmp.mapgui.gui.Resources;
import net.andrews.sooncmp.waypoint.Waypoint;
import net.andrews.sooncmp.waypoint.WaypointManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.mojang.datafixers.util.Pair;

public class SlideshowMenuGui extends MapGuiBase {

    private static byte nav_fillColor = 5;
    private static byte nav_hoverColor = 4;
    private static byte nav_fillTextColor = 84;
    private static byte nav_hoverTextColor = 87;

    private SimpleTextButton backButton;
    private ScrollBar scrollBar;

    private static int offsetX = 10;
    private static int offsetY = 80;
    private static int padding = 3;

    private int pages;
    private int itemsPerPage;

    private List<String> presentations = new ArrayList<>();
    private HashMap<String, MapText> cachedTexts = new HashMap<>();
    private ArrayList<Integer> highlighted = new ArrayList<>();

    public SlideshowMenuGui() {
        backButton = new SimpleTextButton(Resources.back_text, nav_fillColor, nav_hoverColor, nav_fillTextColor,
                nav_hoverTextColor);
        backButton.setClickCallback((ServerPlayerEntity player, Pair<Integer, Integer> mousepos, boolean isInteract,
                SlideshowGUI holder) -> {
            holder.openGui(new MainMenuGUI());
        });
        addInteractableElement(backButton);


        scrollBar = new ScrollBar();

        addInteractableElement(scrollBar);
    }

    public void reloadPresentationsList(SlideshowGUI holder) {
        SlideshowManager manager = SoonCMPMod.slideshowManager;
        List<String> presentations = manager.getPresentationList();
        this.presentations = presentations;
        this.cachedTexts.clear();
    }

    private MapText getText(SlideshowGUI holder, String text) {
        int maxLen = holder.getPanelPixelWidth() / 13;
        String shortened = text;
        if (text.length() > maxLen) {
            // first_part...last_part
            String first_part = text.substring(0, maxLen / 2);
            String last_part = text.substring(text.length() - maxLen / 2);
            shortened = first_part + "..." + last_part;
        }

        MapText mapText = cachedTexts.get(shortened);
        if (mapText == null) {
            mapText = new MapText(shortened, new Font("Arial", Font.PLAIN, 24));
            cachedTexts.put(shortened, mapText);
        }

        return mapText;
    }

    @Override
    public void render(SlideshowGUI holder) {
        MapRenderer.fill(holder, (byte)(MapColors.SNOW + MapColors.BASE_COLOR));

        backButton.setDimensions(10, 10, 100, 50);

        int page = scrollBar.getDisplayPage();

        int heightForItems = holder.getPanelPixelHeight() - offsetY - 5;
        int widthForItems = holder.getPanelPixelWidth() - 2 * offsetX - (pages <= 1 ? 0 : 30);
        int minItemHeight = 40;
        int numItems = this.presentations.size();
        itemsPerPage = (int) Math.floor(heightForItems / minItemHeight);

        pages = (int) Math.ceil((double) numItems / (double) itemsPerPage);

        scrollBar.setDimensions(holder.getPanelPixelWidth() - 30 - 10, 100, 30, holder.getPanelPixelHeight() - 100);
        scrollBar.setTotalPages(pages);

        int boxWidth = widthForItems;
        int boxHeight = heightForItems / itemsPerPage;

        int startIndex = page * itemsPerPage;


        for (int i = 0; i < itemsPerPage; i++) {

            if (i + startIndex >= numItems)
                break;
            String presentation = presentations.get(i + startIndex);
            MapText name = getText(holder, presentation);

            int row = i % itemsPerPage;
            int x = offsetX;
            int y = row * boxHeight + padding + offsetY;
            MapRenderer.fill(holder, x, y, boxWidth, boxHeight - padding * 2,
                    highlighted.contains(i) ? (byte) (MapColors.SNOW + MapColors.DARKER_COLOR) : (byte) (MapColors.SNOW + MapColors.BASE_COLOR));

            MapRenderer.drawText(holder, name, x + padding, y + boxHeight / 2 - name.getHeight() / 2, (byte) 206);

            if (i != itemsPerPage - 1 && i + startIndex != numItems - 1) {
                MapRenderer.fill(holder, x, y + boxHeight - padding, boxWidth, 1, (byte) (MapColors.SNOW + MapColors.DARKER_COLOR));
            }
        }
        MapText titleText = Resources.presentations_title;
        MapRenderer.drawText(holder, titleText, holder.getPanelPixelWidth() / 2 - titleText.getWidth() / 2, 5,
                (byte) 116);
        super.render(holder);
    }


    private int getItemFromPos(SlideshowGUI holder, int x, int y) {
        if (itemsPerPage == 0) {
            return -1;
        }

        int width = holder.getPanelPixelWidth() - offsetX * 2 - (pages <= 1 ? 0 : 30);
        int height = holder.getPanelPixelHeight() - offsetY - 5;

        if (x < offsetX || y < offsetY + padding || y >= height + offsetY - padding
                || x >= offsetX + width) {
            return -1;
        }

        int dy = y - offsetY;

        int boxHeight = height / itemsPerPage;

        int my = dy / boxHeight;

        if (my >= itemsPerPage) {
            return -1;
        }

        int by = my * boxHeight + padding;
        int bh = height / itemsPerPage - padding * 2;

        int bmy = by + bh;

        if (dy < by|| dy >= bmy) {
            return -1;
        }

        return my;
    }

    @Override
    public void onMousePosChange(SlideshowGUI holder, List<Pair<Integer, Integer>> old_positions_looking_at,
            List<Pair<Integer, Integer>> positions_looking_at) {

        highlighted.clear();
        positions_looking_at.forEach(pos -> {
            int box = getItemFromPos(holder, pos.getFirst(), pos.getSecond());
            if (box != -1 && !highlighted.contains(box)) {
                highlighted.add(box);
            }
        });
        super.onMousePosChange(holder, old_positions_looking_at, positions_looking_at);
    }

    @Override
    public void onClick(ServerPlayerEntity player, Pair<Integer, Integer> mousepos, boolean isInteractKey,
            SlideshowGUI holder) {
        int index = getItemFromPos(holder, mousepos.getFirst(), mousepos.getSecond());
        String presentation = getPresentation(index);
        if (presentation != null) {
            holder.openGui(new PresentationGui(presentation, holder));

        }
        super.onClick(player, mousepos, isInteractKey, holder);
    }

    public String getPresentation(int currentBox) {
        if (currentBox != -1) {

            int startIndex = scrollBar.getDisplayPage() * itemsPerPage;
            int index = currentBox + startIndex;

            if (index >= 0 && index < presentations.size()) {
                return presentations.get(index);
            }
        }
        return null;
    }


    @Override
    public void onOpen(SlideshowGUI holder) {
    //    SoonCMPMod.waypointManager.beginTrack(this);
        this.reloadPresentationsList(holder);
        super.onOpen(holder);
    }

    @Override
    public void onClose(SlideshowGUI holder) {
      //  SoonCMPMod.waypointManager.stopTrack(this);
        super.onClose(holder);
    }

    @Override
    public boolean isScrollable() {
        return pages > 1;
    }

}