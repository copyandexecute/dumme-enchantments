package gg.norisk.enchantments.mixin.client;

import gg.norisk.enchantments.EnchantmentRegistry;
import gg.norisk.enchantments.EnchantmentUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Shadow
    @Final
    private GameOptions settings;

    @Shadow
    private static float getMovementMultiplier(boolean bl, boolean bl2) {
        return 0;
    }

    @Inject(method = "tick", at = @At(value = "HEAD", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"), cancellable = true)
    private void stupid$invertedTick(boolean bl, float f, CallbackInfo ci) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) return;
        if (EnchantmentUtils.INSTANCE.getLevel(EnchantmentRegistry.INSTANCE.getInverted(), player.getEquippedStack(EquipmentSlot.FEET)) != null) {
            this.pressingForward = this.settings.backKey.isPressed();
            this.pressingBack = this.settings.forwardKey.isPressed();
            this.pressingLeft = this.settings.rightKey.isPressed();
            this.pressingRight = this.settings.leftKey.isPressed();
            this.movementForward = getMovementMultiplier(this.pressingForward, this.pressingBack);
            this.movementSideways = getMovementMultiplier(this.pressingLeft, this.pressingRight);
            this.jumping = this.settings.jumpKey.isPressed();
            this.sneaking = this.settings.sneakKey.isPressed();
            if (bl) {
                this.movementSideways *= f;
                this.movementForward *= f;
            }
            ci.cancel();
        }
        if (EnchantmentUtils.INSTANCE.getLevel(EnchantmentRegistry.INSTANCE.getInverted(), player.getEquippedStack(EquipmentSlot.LEGS)) != null) {
            this.jumping = this.settings.sneakKey.isPressed();
            this.sneaking = this.settings.jumpKey.isPressed();
            ci.cancel();
        }
    }
}
