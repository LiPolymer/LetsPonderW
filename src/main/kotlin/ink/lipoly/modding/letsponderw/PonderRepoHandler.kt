package ink.lipoly.modding.letsponderw

import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.outputStream

class PonderRepoHandler(
    var repo: String
) {
    companion object {
        val netJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        fun getStringFromWeb(url: String): String {
            val client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build()
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "LetsPonderW-Mod/dev")
                .GET()
                .build()
            return client.send(request, HttpResponse.BodyHandlers.ofString()).body() //todo:异步化
        }

        fun downloadFile(urlString: String, targetPath: Path): Boolean {
            try {
                if (!targetPath.parent.exists()) {
                    targetPath.parent.createDirectories()
                }
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "LetsPonderW-Mod/Dev")
                connection.connectTimeout = 15000
                connection.readTimeout = 60000
                if (connection.responseCode in 200..299) {
                    connection.inputStream.use { input ->
                        targetPath.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    Letsponderw.LOGGER.info("downloaded $targetPath")
                    return true
                } else {
                    Letsponderw.LOGGER.error("download failed ${connection.responseCode}")
                    return false
                }
            } catch (e: Exception) {
                Letsponderw.LOGGER.error("something horrible happened", e)
                return false
            }
        }
    }

    fun getIndex(): PonderRepoIndex {
        return netJson.decodeFromString<PonderRepoIndex>(getStringFromWeb("${repo}/index.json"))
    }
    fun getFragmentPonder(path: String): FragmentMeta {
        return netJson.decodeFromString<FragmentMeta>(getStringFromWeb("${repo}/index/${path}"))
    }

    fun downloadFile(id: String, pathway: String, dist: Path) {
        downloadFile("${repo}/index/${id}/data/${pathway}", dist / pathway)
    }
}