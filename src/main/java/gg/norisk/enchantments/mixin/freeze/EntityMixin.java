package gg.norisk.enchantments.mixin.freeze;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.norisk.enchantments.impl.freeze.FreezeEnchantment;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityMixin implements FreezeEnchantment.IFrozenEntity {
    @Unique
    @Nullable
    private EntityRenderState renderState;

    @Override
    public void setNrc_lastEntityRenderStateLol(@Nullable EntityRenderState renderState) {
        this.renderState = renderState;
    }

    @Override
    public @Nullable EntityRenderState getNrc_lastEntityRenderStateLol() {
        return renderState;
    }

    @WrapOperation(
            method = "playStepSounds",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;playStepSound(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V")
    )
    private void playStepSoundWrapper(Entity instance, BlockPos pos, BlockState state, Operation<Void> original) {
        FreezeEnchantment.INSTANCE.applyStepSound(instance, pos, state, original);
    }
}
