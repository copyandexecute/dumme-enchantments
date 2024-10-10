package gg.norisk.enchantments.mixin;

import gg.norisk.enchantments.impl.ColossalEnchantment;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {
    @Inject(method = "setBlockBreakingInfo", at = @At("HEAD"))
    private void stupid$setBlockBreakingInfo(int i, BlockPos blockPos, int j, CallbackInfo ci) {
        ColossalEnchantment.INSTANCE.spawnBlockBreakingInfos(i,blockPos,j, false);
    }
}
