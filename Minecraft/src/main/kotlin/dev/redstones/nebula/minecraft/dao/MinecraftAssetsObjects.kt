package dev.redstones.nebula.minecraft.dao

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
data class MinecraftAssetsObjects(
    val objects: JsonObject
) {

    val parsedObjects: Map<String, AssetsObject> by lazy { objects.map { it.key to Json.decodeFromJsonElement<AssetsObject>(it.value) }.toMap() }

    @Serializable
    data class AssetsObject(
        val hash: String,
        val size: Long
    )
}
