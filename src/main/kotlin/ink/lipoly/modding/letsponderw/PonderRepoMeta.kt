package ink.lipoly.modding.letsponderw

import kotlinx.serialization.Serializable

@Serializable
data class PonderRepoIndex (
    var items: MutableMap<String,PonderRepoIndexItem>
)

@Serializable
data class PonderRepoIndexItem (
    var type: String,
    var author: String,
    var pathway: String,
    var hash: String
)

@Serializable
data class FragmentMeta(
    var author: String,
    var files: MutableMap<String, String>
)

@Serializable
data class UrlMeta(
    var author: String,
    var url: String,
    var hash: String
)