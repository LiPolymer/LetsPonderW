package ink.lipoly.modding.letsponderw

import kotlinx.serialization.Serializable

@Serializable
data class LocalPonderMeta(
    var includedPonders: MutableMap<String, LocalPonderMetaItem>
)

@Serializable
data class LocalPonderMetaItem(
    var hash: String,
    var files: MutableMap<String, String>
)