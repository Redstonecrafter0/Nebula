package dev.redstones.nebula.jdk.dao

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FooJayPackagesResponse(
    val result: List<FooJayPackage>,
    val message: String = ""
)

@Serializable
data class FooJayPackage(
    val id: String,
    @SerialName("archive_type") val archiveType: String,
    val distribution: String,
    @SerialName("major_version") val majorVersion: Int,
    @SerialName("java_version") val javaVersion: String,
    @SerialName("distribution_version") val distributionVersion: String,
    @SerialName("jdk_version") val jdkVersion: Int,
    @SerialName("latest_build_available") val latestBuildAvailable: Boolean,
    @SerialName("release_status") val releaseStatus: String,
    @SerialName("term_of_support") val termOfSupport: String,
    @SerialName("operating_system") val operatingSystem: String,
    @SerialName("lib_c_type") val libCType: String,
    val architecture: String,
    val fpu: String,
    @SerialName("package_type") val packageType: String,
    @SerialName("javafx_bundled") val javafxBundled: Boolean,
    @SerialName("directly_downloadable") val directlyDownloadable: String,
    val filename: String,
    val links: Links,
    @SerialName("free_use_in_production") val freeUseInProduction: Boolean,
    @SerialName("tck_tested") val tckTested: String,
    @SerialName("tck_cert_uri") val tckCertUri: String,
    @SerialName("aqavit_tested") val aqavitTested: String,
    @SerialName("aqavit_cert_uri") val aqavitCertUri: String,
    val size: Long
) {
    val isLts: Boolean
        get() = termOfSupport == "LTS"

    @Serializable
    data class Links(
        @SerialName("pkg_info_uri") val pkgInfoUri: String,
        @SerialName("pkg_download_redirect") val pkgDownloadRedirect: String
    )
}
