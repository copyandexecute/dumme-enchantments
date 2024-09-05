package gg.norisk.enchantments.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

object ItemStackSerializer : KSerializer<ItemStack> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ItemStack", PrimitiveKind.STRING)

    // Serialisierung von ItemStack zu JsonElement (als String gespeichert)
    override fun serialize(encoder: Encoder, value: ItemStack) {
        encoder.encodeString(Registries.ITEM.getId(value.item).toString())
    }

    // Deserialisierung von JsonElement (String) zu ItemStack
    override fun deserialize(decoder: Decoder): ItemStack {
        return Registries.ITEM.get(Identifier.of(decoder.decodeString())).defaultStack
    }
}
