package net.andrews.mechtour.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.andrews.mechtour.MechTourMod;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;

@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler {
    
    @Inject(method = "onUpdateSelectedSlot",at = @At("HEAD"))
    private void onUpdateSelectedSlotInject(UpdateSelectedSlotC2SPacket packet, CallbackInfo ci) {

        MechTourMod.onUpdateSelectedSlot((ServerPlayNetworkHandler) (Object) this, packet.getSelectedSlot());
    }

    @Inject(method = "onHandSwing", at = @At("HEAD"))
    private void onHandSwingInject(HandSwingC2SPacket packet, CallbackInfo ci) {
        MechTourMod.onSwingClick(((ServerPlayNetworkHandler) (Object)this).player);
    }
}
