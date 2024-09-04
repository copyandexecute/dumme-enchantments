package gg.norisk.enchantments.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.util.math.Vec3d

// Custom Serializer for Vec3d
object Vec3dSerializer : KSerializer<Vec3d> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Vec3d") {
        element<Double>("x")
        element<Double>("y")
        element<Double>("z")
    }

    override fun serialize(encoder: Encoder, value: Vec3d) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeDoubleElement(descriptor, 0, value.x)
        composite.encodeDoubleElement(descriptor, 1, value.y)
        composite.encodeDoubleElement(descriptor, 2, value.z)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): Vec3d {
        val composite = decoder.beginStructure(descriptor)
        var x = 0.0
        var y = 0.0
        var z = 0.0
        loop@ while (true) {
            when (val index = composite.decodeElementIndex(descriptor)) {
                0 -> x = composite.decodeDoubleElement(descriptor, index)
                1 -> y = composite.decodeDoubleElement(descriptor, index)
                2 -> z = composite.decodeDoubleElement(descriptor, index)
                CompositeDecoder.DECODE_DONE -> break@loop
                else -> throw SerializationException("Unknown index $index")
            }
        }
        composite.endStructure(descriptor)
        return Vec3d(x, y, z)
    }
}
