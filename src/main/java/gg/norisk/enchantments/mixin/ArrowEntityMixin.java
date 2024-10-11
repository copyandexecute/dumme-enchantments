package gg.norisk.enchantments.mixin;

import gg.norisk.enchantments.impl.AimBotEnchantment;
import net.minecraft.entity.projectile.ArrowEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArrowEntity.class)
public abstract class ArrowEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void stupid$autoAimTick(CallbackInfo ci) {
        AimBotEnchantment.INSTANCE.handleTick((ArrowEntity) (Object) this, ci);
    }
}
