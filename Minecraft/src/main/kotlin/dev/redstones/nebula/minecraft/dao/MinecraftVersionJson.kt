package dev.redstones.nebula.minecraft.dao

import dev.redstones.nebula.minecraft.dao.MinecraftVersionManifestV2.Type
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

@Serializable
data class MinecraftVersionJson(
    val assetIndex: AssetIndex,
    val downloads: Downloads,
    val libraries: List<Library>,
    val logging: Logging
) {

    @Serializable
    data class AssetIndex(
        val id: String,
        val sha1: String,
        val size: Int,
        val totalSize: Int,
        val url: String
    )

    @Serializable
    data class Downloads(
        val client: Download,
        val server: Download
    ) {

        @Serializable
        data class Download(
            val sha1: String,
            val size: Int,
            val url: String
        )
    }

    @Serializable
    data class Library(
        val downloads: Download,
        val name: String,
        val rules: List<Rule>? = null,
        val natives: Map<String, String> = emptyMap()
    ) {

        @Serializable
        data class Download(
            val artifact: Artifact,
            val classifiers: Map<String, Artifact> = emptyMap()
        ) {

            @Serializable
            data class Artifact(
                val path: String,
                val sha1: String,
                val size: Int,
                val url: String
            )
        }

        @Serializable
        data class Rule(
            val action: Action,
            val os: OS? = null
        ) {

            @Serializable(with = Action.ActionSerializer::class)
            enum class Action(val serialName: String) {
                ALLOW("allow"), DISALLOW("disallow");

                object ActionSerializer : KSerializer<Action> {
                    override val descriptor = PrimitiveSerialDescriptor("action", PrimitiveKind.STRING)

                    override fun serialize(encoder: Encoder, value: Action) {
                        encoder.encodeString(value.serialName)
                    }

                    override fun deserialize(decoder: Decoder): Action {
                        val string = decoder.decodeString()
                        return Action.entries.first { it.serialName == string }
                    }

                }
            }

            @Serializable
            data class OS(
                val name: String? = null,
                val version: String? = null,
                val arch: String? = null
            )
        }
    }

    @Serializable
    data class Logging(
        val client: Client
    ) {

        @Serializable
        data class Client(
            val file: File
        ) {

            @Serializable
            data class File(
                val id: String,
                val sha1: String,
                val size: Int,
                val url: String
            )
        }
    }
}
