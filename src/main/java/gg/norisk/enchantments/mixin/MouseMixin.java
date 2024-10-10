package gg.norisk.enchantments.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import gg.norisk.enchantments.impl.RollEnchantment;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @WrapWithCondition(
            method = "updateMouse",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V")
    )
    private boolean stupid$changeLookDirection(ClientPlayerEntity instance, double x, double y) {
        return !RollEnchantment.INSTANCE.isStupidRolling(instance);
    }
}
