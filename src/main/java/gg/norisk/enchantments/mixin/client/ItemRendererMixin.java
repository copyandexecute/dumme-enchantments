package gg.norisk.enchantments.mixin.client;

import gg.norisk.enchantments.impl.ColossalEnchantment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Inject(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/model/json/Transformation;apply(ZLnet/minecraft/client/util/math/MatrixStack;)V", shift = At.Shift.AFTER)
    )
    private void stupidEnchantments$colossalItems(ItemStack itemStack, ModelTransformationMode modelTransformationMode, boolean bl, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j, BakedModel bakedModel, CallbackInfo ci) {
        ColossalEnchantment.INSTANCE.scaleItem(itemStack, modelTransformationMode, bl, matrixStack, vertexConsumerProvider, i, j, bakedModel);
    }
}
