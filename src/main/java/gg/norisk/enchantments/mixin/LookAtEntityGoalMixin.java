package gg.norisk.enchantments.mixin;

import gg.norisk.enchantments.EnchantmentRegistry;
import gg.norisk.enchantments.EnchantmentUtils;
import gg.norisk.enchantments.impl.MedusaEnchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LookAtEntityGoal.class)
public abstract class LookAtEntityGoalMixin extends Goal {
    @Shadow
    @Nullable
    protected Entity target;

    @Shadow
    @Final
    protected MobEntity mob;

    @Shadow
    private int lookTime;

    @Inject(method = "tick", at = @At("TAIL"))
    private void stupid$tickInjection(CallbackInfo ci) {
        if (this.target instanceof PlayerEntity player) {
            Integer level = EnchantmentUtils.INSTANCE.getLevel(EnchantmentRegistry.INSTANCE.getMedusa(), player.getEquippedStack(EquipmentSlot.HEAD));
            if (level != null && this.lookTime <= 15) {
                MedusaEnchantment.INSTANCE.activateMedusa(mob);
            }
        }
    }
}
