package net.andrews.mechtour.mapgui;

import java.util.ArrayList;
import java.util.Collection;

import net.andrews.mechtour.Utils;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapIcon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class MapGuiMap {

    private int code;
    private ServerPlayerEntity player;

    private ItemFrameEntity mapEntity = null;
    private ItemStack mapItem;
    private CompoundTag mapItemTag;

    public static int MAP_WIDTH = 128;
    public static int MAP_HEIGHT = 128;
    public static byte MAP_SCALE = 0;
    public static Collection<MapIcon> MAP_ICONS = new ArrayList<MapIcon>();

    private byte[] colors;

    private Mutable2DRect changedBounds = new Mutable2DRect(0, 0, 0, 0);
    private byte[] prevColors;

    MapGuiMap(int code, MapGuiHolder holder, ServerPlayerEntity player) {
        this.code = code;
        this.player = player;
        this.colors = new byte[MAP_WIDTH * MAP_HEIGHT];
        this.prevColors = new byte[MAP_WIDTH * MAP_HEIGHT];


        this.mapItem = new ItemStack(Items.FILLED_MAP);
        this.mapItemTag = this.mapItem.getOrCreateTag();
        this.mapItemTag.putInt("map", code);
        this.mapItem.setCount(1);

        this.forceSend();

    }

    public boolean setPixel(int i, int j, byte color) {
        int index = i + j * MAP_WIDTH;
        if (this.colors[index] != color)
            changedBounds.includePos(i, j);
        this.colors[index] = color;
        return true;
    }

    boolean hasUpdates() {
        return changedBounds.getSize() > 0;
    }

    void updateItemFrame(BlockPos pos, Direction side, int x, int y) {

        this.mapEntity = new ItemFrameEntity(player.world, pos, side);
        this.mapEntity.setInvisible(true);
        this.mapEntity.setHeldItemStack(this.mapItem, false);
    }

    void showFrame() {
        Utils.sendPacket(player, new EntitySpawnS2CPacket(this.mapEntity));
        if (!mapEntity.getDataTracker().isEmpty()) {
            Utils.sendPacket(player, new EntityTrackerUpdateS2CPacket(this.mapEntity.getEntityId(),
                    this.mapEntity.getDataTracker(), true));
        }
    }

    void sendMapData() {

        int minX = changedBounds.getMinX();
        int emaxX = changedBounds.getEMaxX();
        int minY = changedBounds.getMinY();
        int emaxY = changedBounds.getEMaxY();

        changedBounds.set(0, 0, 0, 0);

        for (int y = minY; y < emaxY; y++) {
            for (int x = minX; x < emaxX; x++) {

                int index = y * MAP_WIDTH + x;
                if (prevColors[index] != colors[index]) {
                    changedBounds.includePos(x, y);
                }
                prevColors[index] = colors[index];
            }
        }

        Utils.sendPacket(player, new MapUpdateS2CPacket(code, MAP_SCALE, false, false, MAP_ICONS, colors,
                changedBounds.getMinX(), changedBounds.getMinY(), changedBounds.getWidth(), changedBounds.getHeight()));
        changedBounds.set(0, 0, 0, 0);
    }

    void forceSend() {
        Utils.sendPacket(player, new MapUpdateS2CPacket(code, MAP_SCALE, false, false, MAP_ICONS, colors,
                0, 0, MAP_WIDTH, MAP_HEIGHT));
        changedBounds.set(0, 0, 0, 0);
    }
    int getEntityId() {
        return mapEntity.getEntityId();
    }
}
