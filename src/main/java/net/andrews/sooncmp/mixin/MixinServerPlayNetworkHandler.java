package net.andrews.sooncmp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.andrews.sooncmp.SoonCMPMod;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerPlayNetworkHandler {
    
    @Inject(method = "handleSetCarriedItem",at = @At("RETURN"))
    private void onUpdateSelectedSlotInject(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) {

        SoonCMPMod.onUpdateSelectedSlot((ServerGamePacketListenerImpl) (Object) this, packet.getSlot());
    }

    @Inject(method = "handleAnimate", at = @At("RETURN"))
    private void onHandSwingInject(ServerboundSwingPacket packet, CallbackInfo ci) {
        SoonCMPMod.onSwingClick(((ServerGamePacketListenerImpl) (Object)this).player);
    }
}
