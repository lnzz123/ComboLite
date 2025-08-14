import android.content.Context
import com.jctech.plugin.core.installer.InstallerManager
import com.jctech.plugin.core.manager.PluginManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 插件安装工具类
 *
 * 已重构为两个独立方法，以便外部可以分别处理文件发现和单个文件安装。
 */
object InstallUtils {

    /**
     * 从 assets 获取插件文件的路径列表。
     *
     * 该方法会智能判断给定的 assetPath 是目录还是单个文件。
     *
     * @param context Context
     * @param assetPath assets 目录下的路径，例如 "plugins" 或 "plugins/my_plugin.apk"。
     * @return 包含一个或多个插件完整资源路径的列表。如果路径无效或目录为空，则返回空列表。
     */
    fun getAssetPaths(context: Context, assetPath: String): List<String> {
        return try {
            val assetFiles = PluginManager.resourcesManager.getResources().assets.list(assetPath)
            if (assetFiles.isNullOrEmpty()) {
                emptyList()
            } else {
                assetFiles.map { fileName ->
                    if (assetPath.isEmpty()) fileName else "$assetPath/$fileName"
                }
            }
        } catch (e: IOException) {
            try {
                context.assets.open(assetPath).close()
                listOf(assetPath)
            } catch (e: IOException) {
                println("Asset path does not exist or is not accessible: $assetPath")
                emptyList()
            }
        }
    }

    /**
     * 从单个 asset 路径安装一个插件。
     *
     * 此方法包含两个步骤：
     * 1. 将 asset 文件复制到应用的内部存储。
     * 2. 调用插件管理器来安装这个复制后的文件。
     *
     * @param context Context
     * @param assetPath 单个插件文件的完整 assets 路径, 例如 "plugins/my_plugin.apk"。
     * @param forceOverwrite 是否强制覆盖已安装的同名插件。
     * @return 返回 InstallerManager.InstallResult，表示安装成功或失败。
     */
    suspend fun installPluginFromAssets(
        context: Context,
        assetPath: String,
        forceOverwrite: Boolean = false
    ): InstallerManager.InstallResult {
        try {
            val destinationFile = File(context.filesDir, File(assetPath).name)

            PluginManager.resourcesManager.getResources().assets.open(assetPath).use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            return PluginManager.installerManager.installPlugin(destinationFile, forceOverwrite)

        } catch (e: Exception) {
            e.printStackTrace()
            return InstallerManager.InstallResult.Failure("Failed to copy or install asset: $assetPath", e)
        }
    }
}