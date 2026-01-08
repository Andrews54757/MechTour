package net.andrews.sooncmp.mixin;

import java.util.function.BooleanSupplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.andrews.sooncmp.SoonCMPMod;
import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {
    @Inject(method = "tickServer", at = @At("HEAD"))
    private void onBeforeTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {

        SoonCMPMod.onBeforeTick((MinecraftServer) (Object) this);
    }
}
