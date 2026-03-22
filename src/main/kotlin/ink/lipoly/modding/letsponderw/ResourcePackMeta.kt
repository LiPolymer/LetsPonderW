package ink.lipoly.modding.letsponderw
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResourcePackMeta(
    var pack: PackMeta
)

@Serializable
data class PondererPackMeta(
    var pack: PackMeta,
    var ponderer: PondererMeta
)

@Serializable
data class PondererMeta(
    var name: String,
    var version: String,
    var author: String,
    var description: String
)

@Serializable
data class PackMeta(
    @SerialName("pack_format")
    var packFormat: Int,
    var description: String
)