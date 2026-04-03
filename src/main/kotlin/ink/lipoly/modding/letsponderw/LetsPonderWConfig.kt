package ink.lipoly.modding.letsponderw

import kotlinx.serialization.Serializable

@Serializable
data class LetsPonderWConfig(
    var onlyLoad : Boolean = false,
    var normalToast : Boolean = false,
    var repository : String? = null
)