package net.andrews.sooncmp.mapgui;

import java.util.ArrayList;

import net.andrews.sooncmp.Utils;
import net.andrews.sooncmp.mapgui.gui.MapGuiBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

public class EphemeralMapGui {
    private ServerPlayer player;
    private ArrayList<MapGuiMap> maps = new ArrayList<>();
    private boolean panelOpen;

    private BlockPos panelOpenPos;
    private Direction panelFacingSide;
    private ServerLevel panelWorld;

    private AABB panelBox;

    private int panelWidth;
    private int panelHeight;
    private int panelPixelWidth;
    private int panelPixelHeight;
    private int panelSize;
    private BlockPos panelCorner1;
    private BlockPos panelCorner2;
    private int oldMouseX = -1;
    private int oldMouseY = -1;

    private int scrolled = 0;

    private MapGuiBase mapGui;
    private int prevSlot = -1;
    private int lockedSlot = -1;

    private static int PREFIX = 1000000000;

    public EphemeralMapGui(ServerPlayer player) {
        this.player = player;
        this.panelOpen = false;

    }

    public boolean isTrackingPanel() {
        return isPanelOpen() && oldMouseX != -1 && oldMouseY != -1;
    }

    public int getPanelWidth() {
        return panelWidth;
    }

    public int getPanelHeight() {
        return panelHeight;
    }

    public int getPanelPixelWidth() {
        return panelPixelWidth;
    }

    public int getPanelPixelHeight() {
        return panelPixelHeight;
    }

    public boolean shouldRemove() {

        if (!player.isAlive())
            return true;

        return false;
    }

    public boolean shouldClose() {

        if (!player.isAlive())
            return true;

        if (panelWorld != player.level())
            return true;

        if (!player.blockPosition().closerThan(panelOpenPos, 10))
            return true;

        return false;
    }

    public void tick() {
        if (isPanelOpen() && shouldClose()) {
            closePanel();
        }

        
        if (scrolled > 0) scrolled--;

        if (isPanelOpen()) {

            if (this.mapGui != null && this.mapGui.shouldReRender(this)) {
                this.mapGui.render(this);
                this.mapGui.setReRenderFlag(false);
            }
            BlockHitResult result = Utils.raycastBox(player.level(), player, 20, panelBox);

            int newMouseX = -1;
            int newMouseY = -1;
            if (result != null && result.getDirection() == panelFacingSide) {

                double dx = panelCorner1.getX() - result.getLocation().x();
                double dy = panelCorner1.getY() - result.getLocation().y() + 1;
                double dz = panelCorner1.getZ() - result.getLocation().z();

                // dx += 1;
                // dz += 1;

                if (panelFacingSide != Direction.NORTH) {
                    dx = -dx;
                } else {
                    dx += 1;
                }
                if (panelFacingSide != Direction.EAST) {
                    dz = -dz;
                } else {
                    dz += 1;
                }

                newMouseX = (int) ((panelFacingSide.getAxis() == Axis.Z ? dx : dz) * MapGuiMap.MAP_WIDTH);
                newMouseY = (int) (dy * MapGuiMap.MAP_HEIGHT);

            }

            if (newMouseX != oldMouseX || newMouseY != oldMouseY) {

                onMousePosChange(newMouseX, newMouseY, oldMouseX, oldMouseY);
                oldMouseX = newMouseX;
                oldMouseY = newMouseY;
            }
            for (int j = 0; j < panelSize; j++) {
                MapGuiMap map = maps.get(j);
                if (map.hasUpdates()) {
                    map.sendMapData();
                }
            }

        }

    }

    private void onMousePosChange(int newMouseX, int newMouseY, int oldMouseX, int oldMouseY) {
        // System.out.println(newMouseX + " - " + newMouseY);
        if (this.mapGui == null)
            return;
        this.mapGui.onMousePosChange(this, newMouseX, newMouseY, oldMouseX, oldMouseY);

    }

    public void onInteractClick() {
        if (this.mapGui == null)
            return;
        this.mapGui.onClick(true, this);

    }

    public void onSwingClick() {
        if (this.mapGui == null)
            return;
        this.mapGui.onClick(false, this);
    }

    public boolean setPixel(int x, int y, byte color) {
        if (x < 0 || y < 0)
            return false;
        if (x >= panelPixelWidth || y >= panelPixelHeight)
            return false;

        int mapX = x / MapGuiMap.MAP_WIDTH;
        int mapY = y / MapGuiMap.MAP_HEIGHT;

        int index = mapX + mapY * panelWidth;

        return maps.get(index).setPixel(x - mapX * MapGuiMap.MAP_WIDTH, y - mapY * MapGuiMap.MAP_HEIGHT, color);
    }

    public MapGuiBase getGui() {
        return mapGui;
    }

    public boolean isPanelOpen() {
        return panelOpen;
    }

    public void openGui(MapGuiBase gui) {

        if (this.mapGui != null) {
            closeGui();
        }

        MapRenderer.clear(this);

        this.mapGui = gui;
        gui.setReRenderFlag(true);
        gui.onOpen(this);
    }

    public void forceSend() {

        for (MapGuiMap map : maps) {
            map.forceSend();
        }

        if (this.mapGui != null) {
            this.mapGui.setReRenderFlag(true);
        }
    }

    public void closeGui() {
        if (this.mapGui == null)
            return;
        this.mapGui.onClose(this);
        this.mapGui = null;

    }

    public void openPanel(BlockPos openPos, Direction side, int width, int height) {
        if (isPanelOpen()) {
            closePanel();
        }

        if (this.mapGui != null) {
            this.mapGui.setReRenderFlag(true);
        }
        this.panelOpen = true;
        this.panelWorld = this.player.serverLevel();
        this.panelOpenPos = openPos;
        this.panelFacingSide = side;

        this.panelWidth = width;
        this.panelHeight = height;
        this.panelPixelWidth = width * MapGuiMap.MAP_WIDTH;
        this.panelPixelHeight = height * MapGuiMap.MAP_HEIGHT;
        this.panelSize = width * height;

        int offsetX = 0;
        int offsetZ = 0;
        if (side.getAxis() == Axis.Z) {
            offsetX = side != Direction.NORTH ? (-width / 2) : -(-width / 2);
        } else if (side.getAxis() == Axis.X) {
            offsetZ = side != Direction.EAST ? (-width / 2) : -(-width / 2);
        }
        panelCorner1 = new BlockPos(offsetX + openPos.getX(), openPos.getY() + height - 1, offsetZ + openPos.getZ())
                .relative(side.getOpposite(), 1);

        offsetX = 0;
        offsetZ = 0;
        if (side.getAxis() == Axis.Z) {
            offsetX = side != Direction.NORTH ? (width - width / 2 - 1) : -(width - width / 2 - 1);
        } else if (side.getAxis() == Axis.X) {
            offsetZ = side != Direction.EAST ? (width - width / 2 - 1) : -(width - width / 2 - 1);
        }
        panelCorner2 = new BlockPos(offsetX + openPos.getX(), openPos.getY(), offsetZ + openPos.getZ())
                .relative(side.getOpposite(), 1);
        this.panelBox = Utils.createEnclosingAABB(panelCorner1, panelCorner2);

        int index = 0;

        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {

                offsetX = 0;
                offsetZ = 0;
                if (side.getAxis() == Axis.Z) {
                    offsetX = side != Direction.NORTH ? (x - width / 2) : -(x - width / 2);
                } else if (side.getAxis() == Axis.X) {
                    offsetZ = side != Direction.EAST ? (x - width / 2) : -(x - width / 2);
                }
                BlockPos pos = new BlockPos(offsetX + openPos.getX(), openPos.getY() + y, offsetZ + openPos.getZ());

                MapGuiMap map;
                if (index >= maps.size()) {
                    map = new MapGuiMap(new MapId(PREFIX + index), this, player);
                    maps.add(map);
                } else {
                    map = maps.get(index);
                }

                map.updateItemFrame(pos, side, x, y);
                map.showFrame();
                index++;
            }
        }

        forceSend();
    }

    public void closePanel() {
        if (!isPanelOpen())
            return;

        if (oldMouseX != -1 || oldMouseY != -1)
        this.onMousePosChange(-1, -1, oldMouseX, oldMouseY);
        oldMouseX = -1;
        oldMouseY = -1;
        int i = Math.min(this.panelSize, Integer.MAX_VALUE);
        int[] is = new int[i];
        for (int j = 0; j < i; j++) {
            is[j] = maps.get(j).getEntityId();
        }
        Utils.sendPacket(player, new ClientboundRemoveEntitiesPacket(is));
        this.panelOpen = false;
        this.panelWorld = null;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public boolean onUpdateSelectedSlot(ServerGamePacketListenerImpl serverPlayNetworkHandler, int selectedSlot) {
     
        if (!isTrackingPanel() || !isScrollable()) {
            lockedSlot = -1;
        } else if (lockedSlot == -1 && prevSlot != -1) {
            lockedSlot = prevSlot;
        }
        
       
        if (lockedSlot != -1 && scrolled <= 0) {
            serverPlayNetworkHandler.send(new ClientboundSetHeldSlotPacket(lockedSlot));
            
            int prev = lockedSlot - 1;
            int after = lockedSlot + 1;
            if (prev < 0) prev = 8;
            if (after >= 8) after = 0;

            if (selectedSlot == prev) {
                scrolled = 5;
                onScrollUp();
            } else if (selectedSlot == after) {
                scrolled = 5;
                onScrollDown();
            }

        } else if (lockedSlot == -1) {
            prevSlot = selectedSlot;
        }
      
        return lockedSlot != -1;
    }

    private boolean isScrollable() {
        if (this.mapGui == null)
        return false;
        return this.mapGui.isScrollable();
    }

    private void onScrollDown() {
        if (this.mapGui == null)
        return;
        this.mapGui.onScrollDown();
    }

    private void onScrollUp() {
        if (this.mapGui == null)
        return;
        this.mapGui.onScrollUp();
    }
}
