package dev.redstones.nebula.github.dao

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    @SerialName("html_url") val htmlUrl: String,
    val id: Long,
    val author: GitHubUser,
    @SerialName("tag_name") val tagName: String,
    @SerialName("target_commitish") val targetCommitish: String,
    val name: String,
    val draft: Boolean,
    val prerelease: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("published_at") val publishedAt: String,
    val assets: List<Asset>,
    @SerialName("tarball_url") val tarballUrl: String,
    @SerialName("zipball_url") val zipballUrl: String,
    val body: String
) {

    @Serializable
    data class Asset(
        val id: Long,
        val name: String,
        val label: String,
        val uploader: GitHubUser,
        @SerialName("content_type") val contentType: String,
        val state: String,
        val size: Long,
        @SerialName("download_count") val downloadCount: Long,
        @SerialName("created_at") val createdAt: String,
        @SerialName("updated_at") val updatedAt: String,
        @SerialName("browser_download_url") val browserDownloadUrl: String
    )
}
