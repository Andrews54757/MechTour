package net.andrews.sooncmp;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class PlayerInfo {
    public ServerPlayer player;
    public int teleportCooldown = 0;
    public int teleportTimeout = 0;
    public int clickCooldown = 0;
    public ServerLevel world;
    public Vec3 pos;
    public PlayerInfo(ServerPlayer player) {
        this.player = player;
    }
}
