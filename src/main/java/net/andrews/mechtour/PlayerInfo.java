package net.andrews.mechtour;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public class PlayerInfo {
    public ServerPlayerEntity player;
    public int teleportCooldown = 0;
    public int teleportTimeout = 0;
    public ServerWorld world;
    public Vec3d pos;
    public PlayerInfo(ServerPlayerEntity player) {
        this.player = player;
    }
}
