package gg.norisk.enchantments.mixin.client;

import gg.norisk.enchantments.impl.BalloonEnchantment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SinglePartEntityModel.class)
public abstract class SinglePartEntityModelMixin<E extends Entity> extends EntityModel<E> {
    @Inject(method = "render", at = @At("HEAD"))
    private void stupid$BalloonHead(MatrixStack matrixStack, VertexConsumer vertexConsumer, int i, int j, int k, CallbackInfo ci) {
        BalloonEnchantment.INSTANCE.handleSinglePartHead((SinglePartEntityModel<?>) (Object) this,matrixStack,vertexConsumer);
    }
}
