package dev.redstones.nebula.minecraft.dao

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class MinecraftVersionManifestV2(
    val latest: Latest,
    val versions: List<Version>
) {

    @Serializable
    data class Latest(
        val release: String,
        val snapshot: String
    )

    @Serializable
    data class Version(
        val id: String,
        val type: Type,
        val url: String,
        val time: String,
        val releaseTime: String,
        val sha1: String,
        val complianceLevel: Int
    )

    @Serializable(with = Type.TypeSerializer::class)
    enum class Type(val serialName: String) {
        OLD_ALPHA("old_alpha"), OLD_BETA("old_beta"), RELEASE("release"), SNAPSHOT("snapshot");

        object TypeSerializer : KSerializer<Type> {
            override val descriptor = PrimitiveSerialDescriptor("type", PrimitiveKind.STRING)

            override fun serialize(encoder: Encoder, value: Type) {
                encoder.encodeString(value.serialName)
            }

            override fun deserialize(decoder: Decoder): Type {
                val string = decoder.decodeString()
                return Type.entries.first { it.serialName == string }
            }

        }
    }
}
