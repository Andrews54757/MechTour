package net.andrews.sooncmp.mapgui;

import java.util.List;

import net.andrews.sooncmp.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData.MapPatch;

public class MapGuiMap {

    private MapId code;
    private ServerPlayer player;

    private ItemFrame mapEntity = null;
    private ItemStack mapItem;

    public static int MAP_WIDTH = 128;
    public static int MAP_HEIGHT = 128;
    public static byte MAP_SCALE = 0;
    private byte[] colors;

    private Mutable2DRect changedBounds = new Mutable2DRect(0, 0, 0, 0);
    private byte[] prevColors;

    MapGuiMap(MapId code, EphemeralMapGui holder, ServerPlayer player) {
        this.code = code;
        this.player = player;
        this.colors = new byte[MAP_WIDTH * MAP_HEIGHT];
        this.prevColors = new byte[MAP_WIDTH * MAP_HEIGHT];


        this.mapItem = new ItemStack(Items.FILLED_MAP);
        this.mapItem.set(DataComponents.MAP_ID, code);
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

        this.mapEntity = new GlowItemFrame(player.level(), pos, side);
        this.mapEntity.setInvisible(true);
        this.mapEntity.setItem(this.mapItem, false);
    }

    void showFrame() {
        Utils.sendPacket(player, this.mapEntity.getAddEntityPacket(null));
        List<SynchedEntityData.DataValue<?>> list = this.mapEntity.getEntityData().getNonDefaultValues();
        if (list != null && list.size() > 0) {
            Utils.sendPacket(player, new ClientboundSetEntityDataPacket(this.mapEntity.getId(), list));
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

        MapPatch updateData = new MapPatch(changedBounds.getMinX(), changedBounds.getMinY(), changedBounds.getWidth(), changedBounds.getHeight(), colorsToSend);
        Utils.sendPacket(player, new ClientboundMapItemDataPacket(code, MAP_SCALE, false, null, updateData));
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
