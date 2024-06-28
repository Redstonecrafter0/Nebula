package dev.redstones.nebula.github.dao

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubSearchResults(
    @SerialName("total_count") val totalCount: Long,
    @SerialName("incomplete_results") val incompleteResults: Boolean,
    val items: List<Item>
) {

    @Serializable
    data class Item(
        val id: Long,
        val name: String,
        @SerialName("full_name") val fullName: String,
        val private: Boolean,
        val owner: GitHubUser,
        @SerialName("html_url") val htmlUrl: String,
        val description: String,
        val fork: Boolean,
        val url: String,
        @SerialName("created_at") val createdAt: String,
        @SerialName("updated_at") val updatedAt: String,
        @SerialName("pushed_at") val pushedAt: String,
        val homepage: String,
        val size: Long,
        @SerialName("stargazers_count") val stargazersCount: Long,
        @SerialName("watchers_count") val watchersCount: Long,
        val language: String,
        @SerialName("has_issues") val hasIssues: Boolean,
        @SerialName("has_projects") val hasProjects: Boolean,
        @SerialName("has_downloads") val hasDownloads: Boolean,
        @SerialName("has_wiki") val hasWiki: Boolean,
        @SerialName("has_pages") val hasPages: Boolean,
        @SerialName("has_discussions") val hasDiscussions: Boolean,
        @SerialName("forks_count") val forksCount: Long,
        @SerialName("mirror_url") val mirrorUrl: String?,
        val archived: Boolean,
        val disabled: Boolean,
        val license: License?,
        @SerialName("allow_forking") val allowForking: Boolean,
        @SerialName("is_template") val isTemplate: Boolean,
        @SerialName("web_commit_signoff_required") val webCommitSignOffRequired: Boolean,
        val topics: List<String>,
        val visibility: String,
        val forks: Long,
        @SerialName("open_issues") val openIssues: Long,
        val watchers: Long,
        @SerialName("default_branch") val defaultBranch: String,
        val score: Double
    ) {

        @Serializable
        data class License(
            val key: String,
            val name: String,
            @SerialName("spdx_id") val spdxId: String,
            val url: String
        )
    }
}
