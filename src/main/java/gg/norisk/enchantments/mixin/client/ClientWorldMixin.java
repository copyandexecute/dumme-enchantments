package gg.norisk.enchantments.mixin.client;

import gg.norisk.enchantments.impl.ColossalEnchantment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    @Inject(method = "setBlockBreakingInfo", at = @At("HEAD"))
    private void stupid$setBlockBreakingInfo(int i, BlockPos blockPos, int j, CallbackInfo ci) {
        ColossalEnchantment.INSTANCE.spawnBlockBreakingInfos(i, blockPos, j, true);
    }
}
