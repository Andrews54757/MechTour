package net.andrews.sooncmp.mapgui;

import java.util.List;

import net.andrews.sooncmp.Utils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.decoration.GlowItemFrameEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState.UpdateData;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class MapGuiMap {

    private MapIdComponent code;
    private ServerPlayerEntity player;

    private ItemFrameEntity mapEntity = null;
    private ItemStack mapItem;

    public static int MAP_WIDTH = 128;
    public static int MAP_HEIGHT = 128;
    public static byte MAP_SCALE = 0;
    private byte[] colors;

    private Mutable2DRect changedBounds = new Mutable2DRect(0, 0, 0, 0);
    private byte[] prevColors;

    MapGuiMap(MapIdComponent code, MapGuiHolder holder, ServerPlayerEntity player) {
        this.code = code;
        this.player = player;
        this.colors = new byte[MAP_WIDTH * MAP_HEIGHT];
        this.prevColors = new byte[MAP_WIDTH * MAP_HEIGHT];


        this.mapItem = new ItemStack(Items.FILLED_MAP);
        this.mapItem.set(DataComponentTypes.MAP_ID, code);
        this.mapItem.setCount(1);
    }

    public boolean setPixel(int i, int j, byte color) {
        if (i < 0 || i >= MAP_WIDTH || j < 0 || j >= MAP_HEIGHT) {
            throw new IllegalArgumentException("Coordinates out of bounds");
        }
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

        this.mapEntity = new GlowItemFrameEntity(player.getWorld(), pos, side);
        this.mapEntity.setInvisible(true);
        this.mapEntity.setHeldItemStack(this.mapItem, false);
    }

    void showFrame() {
        Utils.sendPacket(player, this.mapEntity.createSpawnPacket(null));
        List<DataTracker.SerializedEntry<?>> list = this.mapEntity.getDataTracker().getChangedEntries();
        if (list != null && list.size() > 0) {
            Utils.sendPacket(player, new EntityTrackerUpdateS2CPacket(this.mapEntity.getId(), list));
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

        int size = changedBounds.getSize();
        if (size == 0) {
            return;
        }
        
        byte[] colorsToSend = new byte[size];
        for (int x = 0; x < changedBounds.getWidth(); x++) {
            for (int y = 0; y < changedBounds.getHeight(); y++) {
                colorsToSend[x + y * changedBounds.getWidth()] = colors[x + minX + (y + minY) * MAP_WIDTH];
            }
        }

        UpdateData updateData = new UpdateData(changedBounds.getMinX(), changedBounds.getMinY(), changedBounds.getWidth(), changedBounds.getHeight(), colorsToSend);
        Utils.sendPacket(player, new MapUpdateS2CPacket(code, MAP_SCALE, false, null, updateData));
        changedBounds.set(0, 0, 0, 0);
    }

    void forceSend() {
        for (int index = 0; index < prevColors.length; index++) {
            prevColors[index] = -1;
        }
        changedBounds.set(0, 0, MAP_WIDTH, MAP_HEIGHT);
    }
    int getEntityId() {
        return mapEntity.getId();
    }
}
