package net.andrews.mechtour.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.andrews.mechtour.MechTourMod;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

@Mixin(ServerPlayerInteractionManager.class)
public class MixinServerPlayerInteractionManager {
    /* 
    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void interactBlockIntercept(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> ci) {

        if (MechTourMod.isGuideItem(stack)) {
            MechTourMod.openGuideGUI(player);
            ci.setReturnValue(ActionResult.SUCCESS);
        }
    }
    */
    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void interactItemIntercept(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, CallbackInfoReturnable<ActionResult> ci) {
        if (hand == Hand.MAIN_HAND) {
            MechTourMod.onInteractClick(player, ci);
        }
    }
}
