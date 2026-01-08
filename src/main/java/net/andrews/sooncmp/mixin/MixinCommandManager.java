package net.andrews.sooncmp.mixin;

import com.mojang.brigadier.CommandDispatcher;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.spongepowered.asm.mixin.injection.At;
import net.andrews.sooncmp.SoonCMPMod;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

@Mixin(Commands.class)
public class MixinCommandManager {
    @Shadow
    @Final
    private CommandDispatcher<CommandSourceStack> dispatcher;
    
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onRegister(Commands.CommandSelection arg, CommandBuildContext commandRegistryAccess, CallbackInfo ci) {
        SoonCMPMod.registerCommands(this.dispatcher);
    }
}
