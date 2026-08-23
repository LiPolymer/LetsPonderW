package ink.lipoly.modding.letsponderw

import ink.lipoly.modding.letsponderw.Letsponderw.ID
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.minecraft.ChatFormatting
import net.minecraft.SharedConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.network.chat.Component
import net.minecraft.server.packs.*
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.PackSource
import net.neoforged.fml.ModList
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.event.AddPackFindersEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.InputStream
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.*
import kotlin.time.Duration.Companion.milliseconds

object PonderPackHandler {
    val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    var config : LetsPonderWConfig? = null

    @OptIn(DelicateCoroutinesApi::class)
    fun onAddPackFinders(event: AddPackFindersEvent) {
        val cfgPath = FMLPaths.CONFIGDIR.get() / "letsPonderW.json"
        if (cfgPath.toFile().exists()) config = json.decodeFromString(cfgPath.readText())
        if (config == null) config = LetsPonderWConfig()
        cfgPath.writeText(json.encodeToString(config))
        if (config!!.repository == null) config!!.repository = "https://gitlab.com/LiPolymer/LetsPonderWIndex/-/raw/main"

        if (event.packType != PackType.CLIENT_RESOURCES) return
        val folder = FMLPaths.GAMEDIR.get() / "letsPonder"
        if (!folder.toFile().exists()) {
            if (!folder.toFile().mkdir()) return // todo: 此处应该发出警示
        }
        initAssembledLetsPonderPackMeta(folder)
        GlobalScope.launch {
            val receipt = assembleSuspend(folder)
            val client = Minecraft.getInstance()
            if (receipt != -1) {
                if (client.overlay == null) {
                    client.execute {
                        client.reloadResourcePacks()
                    }
                    while (client.overlay != null) delay(500.milliseconds)
                    client.toasts.addToast(
                        SystemToast(
                            SystemToast.SystemToastId(10000L),
                            Component.literal("Let's PonderW 已加载 [$receipt]")
                                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                            Component.literal("您可能需要在 Ponderer 重载思索")
                                .withStyle(ChatFormatting.WHITE)
                        )
                    )
                } else if (config!!.normalToast) {
                    client.toasts.addToast(
                        SystemToast(
                            SystemToast.SystemToastId(3000L),
                            Component.literal("Let's PonderW 已加载")
                                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                            Component.literal("载入 $receipt 个思索碎块")
                                .withStyle(ChatFormatting.WHITE)
                        )
                    )
                }
            }
        }
        //addAssembledLetsPonderPack(event, folder)
        //addDownloadedPondererPacks(event, folder)
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
        val packMetaPath = apPath / "pack.mcmeta"
        val ponderMetaPath = apPath / "pack.json"
        val packIconPath = apPath / "pack.png"
        extractAssembleIcon(packIconPath)
        val packMeta = PackMeta(
            SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES),
            "Let's PonderW Assembled Resource")
        packMetaPath.writeText(json.encodeToString(
            ResourcePackMeta(packMeta)
        ))
        //ponderMetaPath.deleteIfExists()
        ponderMetaPath.writeText(json.encodeToString(
            PondererPackMeta(packMeta,
                PondererMeta("lets_ponder_w",
                    (modPath / "assemble.json").md5().substring(0,5),
                    "All Contributors",
                    "Let's PonderW Assembled Ponder")
            )
        ))
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun Path.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(this.readBytes())
        return digest.toHexString()
    }

    suspend fun assembleSuspend(modPath: Path): Int {
        return withContext(Dispatchers.IO) {
            try {
                assemble(modPath)
            } catch (e: Exception) {
                Letsponderw.LOGGER.error(e)
                -1
            }
        }
    }

    fun assemble(modPath: Path): Int {
        val das = modPath / "assembled"
        val dp = das / "data" / "ponderer"
        var lpm = LocalPonderMeta(mutableMapOf())
        val lpf = (modPath / "assemble.json").toFile()
        if (lpf.exists()) lpm = json.decodeFromString(lpf.readText())
        if (!config!!.onlyLoad) {
            Letsponderw.LOGGER.info("assemble process started")
            val allMods = ModList.get().mods
            val rh = PonderRepoHandler(config!!.repository!!)
            val pri = rh.getIndex()

            allMods.forEach { mi ->
                Letsponderw.LOGGER.info("mod: ${mi.modId}")
                try {
                    if (pri.items.contains(mi.modId)) {
                        val pi = pri.items[mi.modId] ?: return@forEach
                        if (lpm.includedPonders.contains(mi.modId)
                            && lpm.includedPonders[mi.modId]?.hash == pi.hash
                        ) return@forEach
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
        }

        Letsponderw.LOGGER.info("deploying...")
        val zipDest = FMLPaths.GAMEDIR.get() / "resourcepacks" / "letsPonderW_assembled.zip"
        zipDest.deleteIfExists()
        das.zipDirectory(zipDest)
        Letsponderw.LOGGER.info("deployed.")

        lpf.writeText(json.encodeToString(lpm))
        return lpm.includedPonders.count()
    }

    private fun Path.zipDirectory(zip: Path) {
        ZipOutputStream(zip.outputStream()).use { zos ->
            toFile().walkTopDown().forEach { file ->
                if (file == toFile()) return@forEach
                val entryName = file.relativeTo(toFile()).toPath().invariantSeparatorsPathString
                if (file.isDirectory) {
                    zos.putNextEntry(ZipEntry("$entryName/"))
                } else {
                    zos.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { it.copyTo(zos) }
                }
                zos.closeEntry()
            }
        }
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