package ink.lipoly.modding.letsponderw

import ink.lipoly.modding.letsponderw.Letsponderw.ID
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.minecraft.ChatFormatting
import net.minecraft.SharedConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.network.chat.Component
import net.minecraft.server.packs.FilePackResources
import net.minecraft.server.packs.PackLocationInfo
import net.minecraft.server.packs.PackSelectionConfig
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.PathPackResources
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.PackSource
import net.neoforged.fml.ModList
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.event.AddPackFindersEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Optional
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.outputStream

object PonderPackHandler {
    val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    // 这下面应该至少有个 index.json（吧?
    const val REPO = "https://gitlab.com/LiPolymer/LetsPonderWIndex/-/raw/main"

    @OptIn(DelicateCoroutinesApi::class)
    fun onAddPackFinders(event: AddPackFindersEvent) {
        if (event.packType != PackType.CLIENT_RESOURCES) return
        val folder = FMLPaths.GAMEDIR.get() / "letsPonder"
        if (!folder.toFile().exists()) {
            if (!folder.toFile().mkdir()) return // todo: 此处应该发出警示
        }
        initAssembledLetsPonderPackMeta(folder)
        GlobalScope.launch {
            val success = assembleSuspend(folder)
            val client = Minecraft.getInstance()
            if (success) {
                if (client.overlay == null) {
                    client.execute {
                        client.reloadResourcePacks()
                    }
                }
                while (client.overlay != null) delay(500)
            }
        }
        addAssembledLetsPonderPack(event, folder)
        addDownloadedPondererPacks(event, folder)
    }

    fun extractAssembleIcon(target: Path) {
        val resourcePath = "/assets/$ID/extract/pack.png"
        val inputStream: InputStream = Letsponderw::class.java.getResourceAsStream(resourcePath) ?: return
        try {
            inputStream.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Letsponderw.LOGGER.error("未能提取包图标", e)
        }
    }
    fun initAssembledLetsPonderPackMeta(modPath: Path) {
        val apPath = modPath / "assembled"
        if (!apPath.exists()) apPath.toFile().mkdir()
        val packMetaFile = (apPath / "pack.mcmeta").toFile()
        val packJsonFile = (apPath / "pack.json").toFile()
        val packIconPath = apPath / "pack.png"
        extractAssembleIcon(packIconPath)
        val packMeta = PackMeta(
            SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES),
            "Let's PonderW Assembled Resource")
        val pondererMeta = PondererMeta(
            "Let's PonderW Assembly",
            "dynamic",
            "All Contributors & LiPolymer",
            "Let'sPw Assembled Ponderer pack"
        )
        packMetaFile.writeText(json.encodeToString(
            ResourcePackMeta(packMeta)
        ))
        packJsonFile.writeText(json.encodeToString(
            PondererPackMeta(packMeta, pondererMeta)
        ))
    }
    suspend fun assembleSuspend(modPath: Path): Boolean {
        return withContext(Dispatchers.Default) {
            try {
                assemble(modPath)
                true
            } catch (e: Exception) {
                Letsponderw.LOGGER.error(e)
                false
            }
        }
    }
    fun assemble(modPath: Path) {
        Letsponderw.LOGGER.info("assemble process started")
        val allMods = ModList.get().mods
        val rh = PonderRepoHandler(REPO)
        val pri = rh.getIndex()
        var lpm = LocalPonderMeta(mutableMapOf())
        val lpf = (modPath / "assemble.json").toFile()
        val dp = modPath / "assembled" / "data" / "ponderer"
        if (lpf.exists()) lpm = json.decodeFromString(lpf.readText())
        allMods.forEach { mi ->
            Letsponderw.LOGGER.info("mod: ${mi.modId}")
            try {
                if (pri.items.contains(mi.modId)) {
                    val pi = pri.items[mi.modId] ?: return
                    if (lpm.includedPonders.contains(mi.modId)
                        && lpm.includedPonders[mi.modId]?.hash == pi.hash) return
                    val fp = rh.getFragmentPonder(pi.pathway)
                    if (lpm.includedPonders.contains(mi.modId)) {
                        val lf = lpm.includedPonders[mi.modId]
                        lf!!.files.toMap().forEach { (path, hash) ->
                            Letsponderw.LOGGER.info("file: $path")
                            if (fp.files.contains(path)) {
                                if (fp.files[path] == hash) fp.files.remove(path)
                                else {
                                    (dp / path).toFile().delete()
                                    lf.files.remove(path)
                                }
                            } else (dp / path).toFile().delete()
                        }
                        fp.files.forEach { (path, hash) ->
                            rh.downloadFile(mi.modId, path, dp)
                            lf.files[path] = hash
                        }
                        lpm.includedPonders[mi.modId]!!.hash = pi.hash
                    } else {
                        fp.files.forEach { (path, _) ->
                            rh.downloadFile(mi.modId, path, dp)
                        }
                        lpm.includedPonders[mi.modId] = LocalPonderMetaItem(pi.hash, fp.files)
                    }
                }
            } catch (e: Exception) {
                Letsponderw.LOGGER.error("something wrong", e)
            }
        }
        Letsponderw.LOGGER.info("${lpm.includedPonders.count()} Ponderer fragment has been assembled")
        lpf.writeText(json.encodeToString(lpm))
    }
    fun addAssembledLetsPonderPack(event: AddPackFindersEvent, modPath: Path) {
        val apPath = modPath / "assembled"
        if (!apPath.toFile().exists()) return
        event.addRepositorySource {
            packConsumer ->
                val packId = "letsPonderAssembledResource"
                val pack = Pack.readMetaAndCreate(
                    PackLocationInfo(
                        packId,
                        Component.literal("Let'sPw Assembled Pack")
                            .withStyle(ChatFormatting.GRAY),
                        PackSource.BUILT_IN,
                        Optional.empty()
                    ),
                    PathPackResources.PathResourcesSupplier(apPath),
                    PackType.CLIENT_RESOURCES,
                    PackSelectionConfig(true, Pack.Position.TOP, false)
                )
                if (pack != null) packConsumer.accept(pack)
        }
    }
    fun addDownloadedPondererPacks(event: AddPackFindersEvent, modPath: Path) {
        val ppPath = modPath / "pondererPacks"
        if (!ppPath.toFile().exists()) return
        ppPath.toFile()
            .listFiles {
                _,name -> name.endsWith(".zip")
            }?.forEach { zipFile -> event.addRepositorySource {
                packConsumer ->
                    val log: Logger = LogManager.getLogger("Let'sPonderW/PackHandler")
                    log.info("Regi [${zipFile.toPath()}]")
                    val packId = "letsPonder_${zipFile.nameWithoutExtension}"
                    val pack = Pack.readMetaAndCreate(
                        PackLocationInfo(
                            packId,
                            Component.literal("Let'sPw External Pack")
                                .withStyle(ChatFormatting.GRAY)
                                .withStyle(ChatFormatting.ITALIC),
                            PackSource.DEFAULT,
                            Optional.empty()
                        ),
                        FilePackResources.FileResourcesSupplier(zipFile),
                        PackType.CLIENT_RESOURCES,
                        PackSelectionConfig(true, Pack.Position.TOP, false)
                    )
                    if (pack != null) packConsumer.accept(pack)
            }
        }
    }
}