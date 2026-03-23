package ink.lipoly.modding.letsponderw

import kotlinx.serialization.Serializable

@Serializable
data class PondererRegistry(
    var packs: MutableMap<String,PondererRegistryItem>
)

@Serializable
data class PondererRegistryItem(
    var name: String = "LetsPonderAssembly",
    var version: String = "dynamic",
    var author: String = "LiPolymer",
    var description: String = "Let\u0027s PonderW Generated Ponderer Pack",
    var sourceFile: String = "",
    var fileHash: String = "",
    var loadedAt: String = "2026-01-24T11:45:14.1919810",
    var packPrefix: String = ""
)
