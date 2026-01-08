package net.andrews.sooncmp.slideshow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.mojang.datafixers.util.Pair;

import net.andrews.sooncmp.Utils;
import net.andrews.sooncmp.mapgui.gui.GuideMenuGUI;
import net.andrews.sooncmp.slideshow.gui.MainMenuGUI;
import net.andrews.sooncmp.slideshow.gui.MapGuiBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

public class SlideshowGUI {
    private HashSet<ServerPlayer> players = new HashSet<>();
    private ArrayList<MapGuiMap> maps = new ArrayList<>();
    private HashMap<Integer, ScrollCache> slot_storage = new HashMap<>();

    private BlockPos panelOpenPos;
    private Direction panelFacingSide;
    private ServerLevel panelWorld;

    private AABB panelBox;
    private AABB panelBoxMirror;

    private int panelWidth;
    private int panelHeight;
    private int panelPixelWidth;
    private int panelPixelHeight;
    private int panelSize;
    private BlockPos panelCorner1;
    private BlockPos panelCorner2;
    private BlockPos mirrorPanelCorner1;
    private BlockPos mirrorPanelCorner2;

    private List<Pair<Integer, Integer>> old_positions_looking_at = new ArrayList<>();

    private MapGuiBase mapGui;

    private static int PREFIX = 2000000000;

    public SlideshowGUI(ServerLevel world, BlockPos openPos, Direction side, int width, int height) {
        openPanel(world, openPos, side, width, height);
        openGui(new MainMenuGUI());
    }

    public SlideshowGUI(MinecraftServer server, SlideshowConfig config) {
        Identifier identifier = Identifier.parse(config.dimension);
        ResourceKey<Level> registryKey = ResourceKey.create(Registries.DIMENSION, identifier);
        ServerLevel world = server.getLevel(registryKey);
        BlockPos pos = new BlockPos(config.x, config.y, config.z);
        openPanel(world, pos, Direction.byName(config.direction), config.width, config.height);
        openGui(new MainMenuGUI());
    }

    public SlideshowConfig getConfig() {
        return new SlideshowConfig(panelWorld.dimension().identifier().getPath(), panelOpenPos.getX(),
                panelOpenPos.getY(), panelOpenPos.getZ(), panelWidth, panelHeight, panelFacingSide.getName());
    }

    public ServerLevel getPanelWorld() {
        return panelWorld;
    }

    public BlockPos getPanelOpenPos() {
        return panelOpenPos;
    }

    public Direction getPanelFacingSide() {
        return panelFacingSide;
    }

    public void addPlayer(ServerPlayer player) {
        if (!players.contains(player)) {
            players.add(player);
            slot_storage.put(player.getId(), new ScrollCache());
            this.maps.forEach(map -> {
                map.showFrameToPlayer(player);
                map.sendMapDataFull(player);
            });
        }
    }

    public void removePlayer(ServerPlayer player) {
        if (players.contains(player)) {
            removePanelFromPlayer(player);
            players.remove(player);
            slot_storage.remove(player.getId());
        }
    }

    public void closePanel() {
        this.players.forEach(player -> {
            removePanelFromPlayer(player);
        });
        this.players.clear();
        this.slot_storage.clear();
        this.maps.clear();
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

    public boolean isPlayerFacingFront(ServerPlayer player) {
        BlockPos playerPos = player.blockPosition();
        BlockPos panelPos = this.panelOpenPos;
        Direction facing = this.panelFacingSide;

        if (facing == Direction.NORTH) {
            if (playerPos.getZ() > panelPos.getZ())
                return false;
        } else if (facing == Direction.SOUTH) {
            if (playerPos.getZ() < panelPos.getZ())
                return false;
        } else if (facing == Direction.EAST) {
            if (playerPos.getX() < panelPos.getX())
                return false;
        } else if (facing == Direction.WEST) {
            if (playerPos.getX() > panelPos.getX())
                return false;
        }

        return true;
    }

    public Pair<Integer, Integer> getMousePosForPlayer(ServerPlayer player) {
        if (isPlayerFacingFront(player)) {
            BlockHitResult result = Utils.raycastBox(player.level(), player, 30, panelBox);
            if (result == null || result.getDirection() != panelFacingSide) {
                return null;
            }

            double dx = panelCorner1.getX() - result.getLocation().x();
            double dy = panelCorner1.getY() - result.getLocation().y() + 1;
            double dz = panelCorner1.getZ() - result.getLocation().z();

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

            int newMouseX = (int) ((panelFacingSide.getAxis() == Axis.Z ? dx : dz) * MapGuiMap.MAP_WIDTH);
            int newMouseY = (int) (dy * MapGuiMap.MAP_HEIGHT);
            return new Pair<>(newMouseX, newMouseY);
        } else {
            BlockHitResult result2 = Utils.raycastBox(player.level(), player, 30,
                    panelBoxMirror);
            if (result2 == null || result2.getDirection() != panelFacingSide.getOpposite()) {
                return null;
            }
            double dx = mirrorPanelCorner1.getX() - result2.getLocation().x();
            double dy = mirrorPanelCorner2.getY() - result2.getLocation().y() + 1;
            double dz = mirrorPanelCorner1.getZ() - result2.getLocation().z();

            if (panelFacingSide.getOpposite() != Direction.NORTH) {
                dx = -dx;
            } else {
                dx += 1;
            }
            if (panelFacingSide.getOpposite() != Direction.EAST) {
                dz = -dz;
            } else {
                dz += 1;
            }

            int newMouseX = (int) ((panelFacingSide.getAxis() == Axis.Z ? dx : dz) * MapGuiMap.MAP_WIDTH);
            int newMouseY = (int) (dy * MapGuiMap.MAP_HEIGHT);

            return new Pair<>(newMouseX, newMouseY);
        }
    }

    public void tick() {

        this.slot_storage.forEach((id, cache) -> {
            if (cache.scrolled > 0)
                cache.scrolled--;
        });

        if (this.mapGui != null && this.mapGui.shouldReRender(this)) {
            this.mapGui.render(this);
            this.mapGui.setReRenderFlag(false);
        }

        List<Pair<Integer, Integer>> positions_looking_at = players.stream().map(player -> getMousePosForPlayer(player))
                .filter(pair -> pair != null).toList();

        // check if positions_looking_at has changed
        if (old_positions_looking_at.size() != positions_looking_at.size() ||
                old_positions_looking_at.stream().anyMatch(pair -> !positions_looking_at.contains(pair))) {
            onMousePosChange(old_positions_looking_at, positions_looking_at);
            old_positions_looking_at = positions_looking_at;
        }

        for (int j = 0; j < panelSize; j++) {
            MapGuiMap map = maps.get(j);
            if (map.hasUpdates()) {
                map.sendMapDataDelta(players);
            }
        }

    }

    private void onMousePosChange(List<Pair<Integer, Integer>> old_positions_looking_at,
            List<Pair<Integer, Integer>> positions_looking_at) {
        // System.out.println(newMouseX + " - " + newMouseY);
        if (this.mapGui == null)
            return;
        this.mapGui.onMousePosChange(this, old_positions_looking_at, positions_looking_at);
    }

    public void onInteractClick(ServerPlayer player) {
        if (this.mapGui == null)
            return;
        Pair<Integer, Integer> mousePos = getMousePosForPlayer(player);
        if (mousePos == null)
            return;
        this.mapGui.onClick(player, mousePos, true, this);
    }

    public void onSwingClick(ServerPlayer player) {
        if (this.mapGui == null)
            return;
        Pair<Integer, Integer> mousePos = getMousePosForPlayer(player);
        if (mousePos == null)
            return;
        this.mapGui.onClick(player, mousePos, false, this);
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

    public void openPanel(ServerLevel world, BlockPos openPos, Direction side, int width, int height) {

        if (this.mapGui != null) {
            this.mapGui.setReRenderFlag(true);
        }
        this.maps.clear();
        this.panelWorld = world;
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
            offsetX = side != Direction.NORTH ? (0) : (width - 1);
        } else if (side.getAxis() == Axis.X) {
            offsetZ = side != Direction.EAST ? (0) : (width - 1);
        }
        panelCorner1 = new BlockPos(offsetX + openPos.getX(), openPos.getY() + height - 1, offsetZ + openPos.getZ())
                .relative(side.getOpposite(), 1);

        offsetX = 0;
        offsetZ = 0;
        if (side.getAxis() == Axis.Z) {
            offsetX = side != Direction.NORTH ? (width - 1) : 0;
        } else if (side.getAxis() == Axis.X) {
            offsetZ = side != Direction.EAST ? (width - 1) : 0;
        }
        panelCorner2 = new BlockPos(offsetX + openPos.getX(), openPos.getY(), offsetZ + openPos.getZ())
                .relative(side.getOpposite(), 1);

        mirrorPanelCorner1 = panelCorner2.relative(side, 1);
        mirrorPanelCorner2 = panelCorner1.relative(side, 1);

        this.panelBox = Utils.createEnclosingAABB(panelCorner1, panelCorner2);
        this.panelBoxMirror = Utils.createEnclosingAABB(mirrorPanelCorner1, mirrorPanelCorner2);

        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {

                offsetX = 0;
                offsetZ = 0;
                if (side.getAxis() == Axis.Z) {
                    offsetX = side != Direction.NORTH ? (x) : (width - x - 1);
                } else if (side.getAxis() == Axis.X) {
                    offsetZ = side != Direction.EAST ? (x) : (width - x - 1);
                }
                BlockPos pos = new BlockPos(offsetX + openPos.getX(), openPos.getY() + y, offsetZ + openPos.getZ());

                offsetX = 0;
                offsetZ = 0;
                if (side.getAxis() == Axis.Z) {
                    offsetX = side != Direction.NORTH ? (width - x - 1) : (x);
                } else if (side.getAxis() == Axis.X) {
                    offsetZ = side != Direction.EAST ? (width - x - 1) : (x);
                }
                BlockPos posMirrored = (new BlockPos(offsetX + openPos.getX(), openPos.getY() + y,
                        offsetZ + openPos.getZ()))
                        .relative(side.getOpposite(), 1);

                MapGuiMap map = new MapGuiMap(new MapId(PREFIX++), this.panelWorld, pos, posMirrored, side);
                maps.add(map);
            }
        }

        players.forEach(player -> {
            maps.forEach(map -> map.showFrameToPlayer(player));
        });

        forceSend();
    }

    void removePanelFromPlayer(ServerPlayer player) {
        int i = Math.min(this.panelSize, Integer.MAX_VALUE);
        int[] is = new int[i * 2];
        for (int j = 0; j < i; j++) {
            is[j * 2] = maps.get(j).getFrameIDS().getFirst();
            is[j * 2 + 1] = maps.get(j).getFrameIDS().getSecond();
        }
        Utils.sendPacket(player, new ClientboundRemoveEntitiesPacket(is));
    }

    public boolean onUpdateSelectedSlot(ServerGamePacketListenerImpl serverPlayNetworkHandler, int selectedSlot) {
        Pair<Integer, Integer> mousePos = getMousePosForPlayer(serverPlayNetworkHandler.player);
        ScrollCache scrollCache = slot_storage.get(serverPlayNetworkHandler.player.getId());

        if (mousePos == null || !isScrollable()) {
            scrollCache.lockedSlot = -1;
        } else if (scrollCache.lockedSlot == -1 && scrollCache.prevSlot != -1) {
            scrollCache.lockedSlot = scrollCache.prevSlot;
        }

        if (scrollCache.lockedSlot != -1 && selectedSlot != scrollCache.lockedSlot) {
            serverPlayNetworkHandler.send(new ClientboundSetHeldSlotPacket(scrollCache.lockedSlot));
        }

        if (scrollCache.lockedSlot != -1 && scrollCache.scrolled <= 0) {

            int prev = scrollCache.lockedSlot - 1;
            int after = scrollCache.lockedSlot + 1;
            if (prev < 0)
                prev = 8;
            if (after >= 8)
                after = 0;

            if (selectedSlot == prev) {
                scrollCache.scrolled = 5;
                onScrollUp();
            } else if (selectedSlot == after) {
                scrollCache.scrolled = 5;
                onScrollDown();
            }

        } else if (scrollCache.lockedSlot == -1) {
            scrollCache.prevSlot = selectedSlot;
        }

        return scrollCache.lockedSlot != -1;
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
