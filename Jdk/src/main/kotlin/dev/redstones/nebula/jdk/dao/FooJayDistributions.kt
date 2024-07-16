package dev.redstones.nebula.jdk.dao

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FooJayDistributionsResponse(
    val result: List<FooJayDistribution>,
    val message: String = ""
)

@Serializable
data class FooJayDistribution(
    val name: String,
    @SerialName("api_parameter") val apiParameter: String,
    val maintained: Boolean,
    val available: Boolean,
    @SerialName("build_of_openjdk") val buildOfOpenJdk: Boolean,
    @SerialName("build_of_graalvm") val buildOfGraalVm: Boolean,
    @SerialName("official_url") val officialUrl: String,
    val synonyms: Set<String>,
    val versions: List<String>
)
