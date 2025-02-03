package net.andrews.sooncmp.slideshow;

import com.mojang.datafixers.util.Pair;

import net.minecraft.server.network.ServerPlayerEntity;

public interface IClickCallback {
    void call(ServerPlayerEntity player, Pair<Integer, Integer> mousepos, boolean isInteractKey, SlideshowGUI holder);
}
