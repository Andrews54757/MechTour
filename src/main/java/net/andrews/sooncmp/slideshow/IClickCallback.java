package net.andrews.sooncmp.slideshow;

import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerPlayer;

public interface IClickCallback {
    void call(ServerPlayer player, Pair<Integer, Integer> mousepos, boolean isInteractKey, SlideshowGUI holder);
}
