package net.andrews.mechtour.mixin;

import java.util.function.BooleanSupplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.andrews.mechtour.MechTourMod;
import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onBeforeTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {

        MechTourMod.onBeforeTick((MinecraftServer) (Object) this);
    }
}
