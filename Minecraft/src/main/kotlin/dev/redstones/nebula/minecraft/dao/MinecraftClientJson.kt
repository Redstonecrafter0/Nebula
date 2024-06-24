package dev.redstones.nebula.minecraft.dao

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

@Serializable
data class MinecraftClientJson(
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
        val rules: JsonArray? = null
    ) {

        @Serializable
        data class Download(
            val artifact: Artifact
        ) {

            @Serializable
            data class Artifact(
                val path: String,
                val sha1: String,
                val size: Int,
                val url: String
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
