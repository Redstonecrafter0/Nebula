package dev.redstones.nebula.jdk.dao

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FooJayMajorVersionsResponse(
    val result: List<FooJayMajorVersion?>,
    val message: String = ""
)

@Serializable
data class FooJayMajorVersion(
    @SerialName("major_version") val majorVersion: Int,
    @SerialName("term_of_support") val termOfSupport: String,
    val maintained: Boolean,
    @SerialName("early_access_only") val earlyAccessOnly: Boolean,
    @SerialName("release_status") val releaseStatus: String
) {
    val isLts: Boolean
        get() = termOfSupport == "LTS"
}
