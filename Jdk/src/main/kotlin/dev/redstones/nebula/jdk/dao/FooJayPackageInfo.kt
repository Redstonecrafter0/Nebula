package dev.redstones.nebula.jdk.dao

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FooJayPackageInfoResponse(
    val result: List<FooJayPackageInfo?>,
    val message: String = ""
)

@Serializable
data class FooJayPackageInfo(
    val filename: String,
    @SerialName("direct_download_uri") val directDownloadUri: String,
    @SerialName("download_site_uri") val downloadSiteUri: String,
    @SerialName("signature_uri") val signatureUri: String,
    @SerialName("checksum_uri") val checksumUri: String,
    val checksum: String,
    @SerialName("checksum_type") val checksumType: String
)
