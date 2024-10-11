package gg.norisk.enchantments.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import gg.norisk.enchantments.EnchantmentRegistry;
import gg.norisk.enchantments.EnchantmentUtils;
import gg.norisk.enchantments.impl.RollEnchantment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @WrapWithCondition(
            method = "updateMouse",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V")
    )
    private boolean stupid$changeLookDirection(ClientPlayerEntity instance, double x, double y) {
        return !RollEnchantment.INSTANCE.isStupidRolling(instance);
    }

    @ModifyArgs(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
    private void stupid$invertedMouse(Args args) {
        var player = client.player;
        if (player == null) return;
        if (EnchantmentUtils.INSTANCE.getLevel(EnchantmentRegistry.INSTANCE.getInverted(), player.getEquippedStack(EquipmentSlot.HEAD)) != null) {
            double first = args.get(0);
            double second = args.get(1);
            args.setAll(first * -1, second * -1);
        }
    }
}
