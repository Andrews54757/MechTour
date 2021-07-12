package net.andrews.mechtour.mapgui.gui;

import net.andrews.mechtour.MechTourMod;
import net.andrews.mechtour.mapgui.MapGuiHolder;
import net.andrews.mechtour.mapgui.MapRenderer;
import net.andrews.mechtour.mapgui.MapText;
import net.andrews.mechtour.waypoint.Waypoint;
import net.andrews.mechtour.waypoint.WaypointIcons;
import net.andrews.mechtour.waypoint.WaypointManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;

public class WaypointsMenuGui extends MapGuiBase {

    private static byte tab_fillTextColor = 84;

    private static byte tab_hoverTextColor = 96;
    private static byte nav_fillColor = 5;
    private static byte nav_hoverColor = 4;
    private static byte nav_fillTextColor = 84;
    private static byte nav_hoverTextColor = 87;

    private static byte overworld_color = 76;
    private static byte nether_color = 114;
    private static byte end_color = 72;

    private int[] pageHolder;

    private DimensionTab currentDimensionTab = null;
    private SimpleTextButton backButton;
    private SimpleTextButton closeButton;
    private ScrollBar scrollBar;

    private static int rows = 3;
    private static int columns = 4;
    private static int offsetX = 15;
    private static int offsetY = 105;

    private static int padding = 3;
    private static int itemsPerPage = rows * columns;

    private int pages;
    private int currentBox = -1;

    SimpleTextButton dimension_overworld_button = new SimpleTextButton(Resources.dimension_overworld_title,
            overworld_color, overworld_color, tab_fillTextColor, tab_hoverTextColor);
    SimpleTextButton dimension_nether_button = new SimpleTextButton(Resources.dimension_nether_title, nether_color,
            nether_color, tab_fillTextColor, tab_hoverTextColor);
    SimpleTextButton dimension_end_button = new SimpleTextButton(Resources.dimension_end_title, end_color, end_color,
            tab_fillTextColor, tab_hoverTextColor);

    public WaypointsMenuGui() {
        pageHolder = new int[] { 0, 0, 0 };
        backButton = new SimpleTextButton(Resources.back_text, nav_fillColor, nav_hoverColor, nav_fillTextColor,
                nav_hoverTextColor);
        backButton.setClickCallback((boolean isInteract, MapGuiHolder holder) -> {
            holder.openGui(new GuideMenuGUI());
        });

        closeButton = new SimpleTextButton(Resources.close_text, nav_fillColor, nav_hoverColor, nav_fillTextColor,
                nav_hoverTextColor);
        closeButton.setClickCallback((boolean isInteract, MapGuiHolder holder) -> {
            holder.closePanel();
        });
        addInteractableElement(backButton);
        addInteractableElement(closeButton);

        dimension_overworld_button = new SimpleTextButton(Resources.dimension_overworld_title, overworld_color,
                overworld_color, tab_fillTextColor, tab_hoverTextColor);
        dimension_nether_button = new SimpleTextButton(Resources.dimension_nether_title, nether_color, nether_color,
                tab_fillTextColor, tab_hoverTextColor);
        dimension_end_button = new SimpleTextButton(Resources.dimension_end_title, end_color, end_color,
                tab_fillTextColor, tab_hoverTextColor);

        dimension_overworld_button.setClickCallback((boolean isInteract, MapGuiHolder holder) -> {
            setDimensionTab(DimensionTab.OVERWORLD);
        });
        dimension_nether_button.setClickCallback((boolean isInteract, MapGuiHolder holder) -> {
            setDimensionTab(DimensionTab.NETHER);
        });
        dimension_end_button.setClickCallback((boolean isInteract, MapGuiHolder holder) -> {
            setDimensionTab(DimensionTab.END);
        });
        addInteractableElement(dimension_overworld_button);
        addInteractableElement(dimension_nether_button);
        addInteractableElement(dimension_end_button);

        scrollBar = new ScrollBar();

        addInteractableElement(scrollBar);
    }

    @Override
    public void render(MapGuiHolder holder) {
        if (currentDimensionTab == null)
            setDimensionTab(DimensionTab.getDimensionTab(holder.getPlayer().getServerWorld()));
        MapRenderer.fill(holder, (byte) 0);
        // banner = new BitMapImage("banner.png").scaledDimensions(-1,
        // 100).setAlphaCutoff(200).bake();

        int rect_width = holder.getPanelPixelWidth() - 20;

        MapRenderer.fill(holder, 10, 100, holder.getPanelPixelWidth() - 20, holder.getPanelPixelHeight() - 100,
                currentDimensionTab.getColor());

        int div = rect_width / 3;
        dimension_overworld_button.setDimensions(10, 65, div, 35);
        dimension_nether_button.setDimensions(10 + div, 65, div, 35);
        dimension_end_button.setDimensions(10 + div * 2, 65, div, 35);

        backButton.setDimensions(10, 10, 100, 50);
        closeButton.setDimensions(holder.getPanelPixelWidth() - 100 - 10, 10, 100, 50);

        WaypointManager manager = MechTourMod.waypointManager;
        ArrayList<Waypoint> waypoints = manager.getWaypoints(currentDimensionTab.getKey().getValue().getPath());

        int page = scrollBar.getDisplayPage();

        pages = (int) Math.ceil((double) waypoints.size() / (double) itemsPerPage);
        scrollBar.setDimensions(holder.getPanelPixelWidth() - 30 - 10, 100, 30, holder.getPanelPixelHeight() - 100);
        scrollBar.setTotalPages(pages);

        int width = holder.getPanelPixelWidth() - 30 - (pages <= 1 ? 0 : 30);
        int height = holder.getPanelPixelHeight() - 110;

        int boxWidth = width / columns;
        int boxHeight = height / rows;

        int bw = width / columns - padding * 2;
        int bh = height / rows - padding * 2;

        int startIndex = page * itemsPerPage;

        MapText titleText = Resources.waypoints_title;
        for (int i = 0; i < itemsPerPage; i++) {

            if (i + startIndex >= waypoints.size())
                break;
            Waypoint waypoint = waypoints.get(i + startIndex);
            MapText name = waypoint.getTextIcon();
            WaypointIcons.Icon icon = waypoint.getIcon();

            int row = i / columns;
            int column = i % columns;

            int x = column * boxWidth + offsetX + padding;
            int y = row * boxHeight + offsetY + padding;
            MapRenderer.fill(holder, x, y, boxWidth - padding * 2, boxHeight - padding * 2,
                    currentBox == i ? (byte) 40 : (byte) 41);

            if (icon != null)
                MapRenderer.drawImage(holder, icon.getImage(), x + bw / 2 - icon.getImage().getWidth() / 2, y + 3);

            MapRenderer.drawText(holder, name, x + bw / 2 - name.getWidth() / 2, y + bh - name.getHeight() - 5,
                    (byte) 206);

            if (currentBox == i) {
                titleText = waypoint.getPosText();
            }
        }

        MapRenderer.drawText(holder, titleText, holder.getPanelPixelWidth() / 2 - titleText.getWidth() / 2, 5,
                (byte) 116);

        super.render(holder);
    }

    private void setDimensionTab(DimensionTab tab) {

        if (currentDimensionTab != null) {
            int page = scrollBar.getCurrentPage();
            pageHolder[currentDimensionTab.getIndex()] = page;
        }
        this.currentDimensionTab = tab;
        scrollBar.setCurrentPage(pageHolder[currentDimensionTab.getIndex()]);
        this.setReRenderFlag(true);

    }

    private int getItemFromPos(MapGuiHolder holder, int x, int y) {
        int width = holder.getPanelPixelWidth() - 30 - (pages <= 1 ? 0 : 30);
        int height = holder.getPanelPixelHeight() - 110;

        if (x < offsetX + padding || y < offsetY + padding || y >= holder.getPanelPixelHeight() - padding
                || x >= holder.getPanelPixelWidth() - padding) {
            return -1;
        }

        int dx = x - offsetX;
        int dy = y - offsetY;
        int boxWidth = width / columns;
        int boxHeight = height / rows;

        int mx = dx / boxWidth;
        int my = dy / boxHeight;

        if (mx >= columns || my >= rows) {
            return -1;
        }

        int bx = mx * boxWidth + padding;
        int by = my * boxHeight + padding;
        int bw = width / columns - padding * 2;
        int bh = height / rows - padding * 2;

        int bmx = bx + bw;
        int bmy = by + bh;

        if (dx < bx || dy < by || dx >= bmx || dy >= bmy) {
            return -1;
        }

        return mx + my * columns;
    }

    @Override
    public void onMousePosChange(MapGuiHolder holder, int newMouseX, int newMouseY, int oldMouseX, int oldMouseY) {
        // TODO Auto-generated method stub

        int box = this.getItemFromPos(holder, newMouseX, newMouseY);
        if (box != currentBox) {
            currentBox = box;
            this.setReRenderFlag(true);
        }
        super.onMousePosChange(holder, newMouseX, newMouseY, oldMouseX, oldMouseY);
    }

    @Override
    public void onClick(boolean isInteractKey, MapGuiHolder holder) {
        // TODO Auto-generated method stub
        Waypoint waypoint = getWaypoint();
        if (waypoint != null) {

            MechTourMod.teleportToWaypoint(holder.getPlayer(), waypoint, true);

        }
        super.onClick(isInteractKey, holder);
    }

    public Waypoint getWaypoint() {
        if (currentBox != -1) {

            WaypointManager manager = MechTourMod.waypointManager;
            ArrayList<Waypoint> waypoints = manager.getWaypoints(currentDimensionTab.getKey().getValue().getPath());

            int startIndex = scrollBar.getDisplayPage() * itemsPerPage;
            int index = currentBox + startIndex;

            if (index >= 0 && index < waypoints.size()) {
                Waypoint waypoint = waypoints.get(index);
                return waypoint;
            }
        }
        return null;
    }

    public enum DimensionTab {
        OVERWORLD(DimensionType.OVERWORLD_ID, (byte) 76, 0), NETHER(DimensionType.THE_NETHER_ID, (byte) 114, 1),
        END(DimensionType.THE_END_ID, (byte) 72, 2);

        private byte color;
        private RegistryKey<World> key;
        private int index;

        private DimensionTab(Identifier identifier, byte color, int index) {
            key = RegistryKey.of(Registry.DIMENSION, identifier);
            this.color = color;
            this.index = index;
        }

        public byte getColor() {
            return color;
        }

        public int getIndex() {
            return index;
        }

        public RegistryKey<World> getKey() {
            return key;
        }

        public static DimensionTab getDimensionTab(World world) {
            for (DimensionTab tab : DimensionTab.values()) {
                if (tab.getKey() == world.getRegistryKey()) {
                    return tab;
                }
            }
            return OVERWORLD;
        }
    }

    @Override
    public void onOpen(MapGuiHolder holder) {
        // TODO Auto-generated method stub

        MechTourMod.waypointManager.beginTrack(this);
        super.onOpen(holder);
    }

    @Override
    public void onClose(MapGuiHolder holder) {
        // TODO Auto-generated method stub
        MechTourMod.waypointManager.stopTrack(this);
        super.onClose(holder);
    }

    @Override
    public boolean isScrollable() {
        return pages > 1;
    }

}