package gg.norisk.enchantments.mixin.freeze;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import gg.norisk.enchantments.impl.freeze.FreezeEnchantment;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Shadow
    public abstract S createRenderState();

    @Shadow
    public abstract void updateRenderState(T entity, S state, float tickProgress);

    @ModifyReturnValue(
            method = "getAndUpdateRenderState",
            at = @At("RETURN")
    )
    private S freeze$copyRenderState(S original, T entity, float tickProgress) {
        if (FreezeEnchantment.INSTANCE.getNrc_isFrozen(entity)) {
            var state = ((FreezeEnchantment.IFrozenEntity) entity).getNrc_lastEntityRenderStateLol();
            if (state != null) {
                return (S) state;
            } else {
                state = this.createRenderState();
                this.updateRenderState(entity, (S) state, tickProgress);
                FreezeEnchantment.INSTANCE.modifyRenderState(state);
                ((FreezeEnchantment.IFrozenEntity) entity).setNrc_lastEntityRenderStateLol(state);
                return (S) state;
            }
        } else {
            ((FreezeEnchantment.IFrozenEntity) entity).setNrc_lastEntityRenderStateLol(null);
        }
        //FreezeEnchantment.INSTANCE.copyRenderState(original, entity, tickProgress);
        return original;
    }
}
