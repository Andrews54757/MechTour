package net.andrews.sooncmp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.andrews.sooncmp.SoonCMPMod;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;

@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler {
    
    @Inject(method = "onUpdateSelectedSlot",at = @At("RETURN"))
    private void onUpdateSelectedSlotInject(UpdateSelectedSlotC2SPacket packet, CallbackInfo ci) {

        SoonCMPMod.onUpdateSelectedSlot((ServerPlayNetworkHandler) (Object) this, packet.getSelectedSlot());
    }

    @Inject(method = "onHandSwing", at = @At("RETURN"))
    private void onHandSwingInject(HandSwingC2SPacket packet, CallbackInfo ci) {
        SoonCMPMod.onSwingClick(((ServerPlayNetworkHandler) (Object)this).player);
    }
}
