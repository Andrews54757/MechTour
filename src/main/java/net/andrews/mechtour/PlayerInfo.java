package net.andrews.mechtour;

import org.apache.logging.log4j.core.jmx.Server;

import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerInfo {
    public ServerPlayerEntity player;
    public int teleportCooldown = 0;
    public int teleportTimeout = 0;
    public PlayerInfo(ServerPlayerEntity player) {
        this.player = player;
    }
}
