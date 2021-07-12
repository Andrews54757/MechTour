package net.andrews.mechtour.mapgui;

import java.util.ArrayList;

import net.andrews.mechtour.Utils;
import net.andrews.mechtour.mapgui.gui.MapGuiBase;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.HeldItemChangeS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;

public class MapGuiHolder {
    private ServerPlayerEntity player;
    private ArrayList<MapGuiMap> maps = new ArrayList<>();
    private boolean panelOpen;

    private BlockPos panelOpenPos;
    private Direction panelFacingSide;
    private ServerWorld panelWorld;

    private Box panelBox;

    private int panelWidth;
    private int panelHeight;
    private int panelPixelWidth;
    private int panelPixelHeight;
    private int panelSize;
    private BlockPos panelCorner1;
    private BlockPos panelCorner2;
    private int oldMouseX = -1;
    private int oldMouseY = -1;

    private int clicked = 0;

    private MapGuiBase mapGui;
    private int prevSlot = -1;
    private int lockedSlot = -1;

    private static int PREFIX = 1000000000;

    public MapGuiHolder(ServerPlayerEntity player) {
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

        if (panelWorld != player.getServerWorld())
            return true;

        if (!player.getBlockPos().isWithinDistance(panelOpenPos, 10))
            return true;

        return false;
    }

    public void tick() {
        if (isPanelOpen() && shouldClose()) {
            closePanel();
        }

        // ImageToMapProcessor.getBestColor(200, 0, 0, 255);
        if (clicked > 0)
            clicked--;
        if (isPanelOpen()) {

            if (this.mapGui != null && this.mapGui.shouldReRender(this)) {
                this.mapGui.render(this);
                this.mapGui.setReRenderFlag(false);
            }
            BlockHitResult result = Utils.raycastBox(player.world, player, 20, panelBox);

            int newMouseX = -1;
            int newMouseY = -1;
            if (result != null && result.getSide() == panelFacingSide) {

                double dx = panelCorner1.getX() - result.getPos().getX();
                double dy = panelCorner1.getY() - result.getPos().getY() + 1;
                double dz = panelCorner1.getZ() - result.getPos().getZ();

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
        if (this.mapGui == null || clicked != 0)
            return;
        clicked = 5;
        this.mapGui.onClick(true, this);

    }

    public void onSwingClick() {
        if (this.mapGui == null || clicked != 0)
            return;
        clicked = 5;
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

        MapRenderer.fill(this, (byte) 0);

        this.mapGui = gui;
        gui.onOpen(this);

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
        this.panelWorld = this.player.getServerWorld();
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
                .offset(side.getOpposite(), 1);

        offsetX = 0;
        offsetZ = 0;
        if (side.getAxis() == Axis.Z) {
            offsetX = side != Direction.NORTH ? (width - width / 2 - 1) : -(width - width / 2 - 1);
        } else if (side.getAxis() == Axis.X) {
            offsetZ = side != Direction.EAST ? (width - width / 2 - 1) : -(width - width / 2 - 1);
        }
        panelCorner2 = new BlockPos(offsetX + openPos.getX(), openPos.getY(), offsetZ + openPos.getZ())
                .offset(side.getOpposite(), 1);
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
                    map = new MapGuiMap(PREFIX + index, this, player);
                    maps.add(map);
                } else {
                    map = maps.get(index);
                }

                map.updateItemFrame(pos, side, x, y);
                map.showFrame();
                index++;
            }
        }
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
        Utils.sendPacket(player, new EntitiesDestroyS2CPacket(is));
        this.panelOpen = false;
        this.panelWorld = null;
    }

    public ServerPlayerEntity getPlayer() {
        return player;
    }

    public boolean onUpdateSelectedSlot(ServerPlayNetworkHandler serverPlayNetworkHandler, int selectedSlot) {
     
        if (!isTrackingPanel() || !isScrollable()) {
            lockedSlot = -1;
        } else if (lockedSlot == -1 && prevSlot != -1) {
            lockedSlot = prevSlot;
        }
        
       
        if (lockedSlot != -1 && clicked == 0) {
            serverPlayNetworkHandler.sendPacket(new HeldItemChangeS2CPacket(lockedSlot));
            
            int prev = lockedSlot - 1;
            int after = lockedSlot + 1;
            if (prev < 0) prev = 8;
            if (after >= 8) after = 0;

            if (selectedSlot == prev) {
                clicked = 5;
                onScrollUp();
            } else if (selectedSlot == after) {
                clicked = 5;
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
