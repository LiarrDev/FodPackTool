package com.fodsdk.games

import com.fodsdk.com.fodsdk.utils.FileUtil
import com.fodsdk.com.fodsdk.utils.replace
import com.fodsdk.utils.AndroidXmlHandler
import com.fodsdk.utils.DrawableUtil
import com.fodsdk.utils.command
import java.io.File

abstract class Game(private val apk: String) {

    protected lateinit var decompileDir: String
    private lateinit var apktool: String
    private lateinit var manifestFile: File

    /**
     * 反编译
     */
    fun decompile(decompileDir: String, apktool: String): Boolean {
        this.decompileDir = decompileDir
        this.apktool = apktool
        this.manifestFile = File(decompileDir, "AndroidManifest.xml")
        val apkFile = File(apk)
        return if (apkFile.exists() && apkFile.isFile) {
            val decompileFile = File(decompileDir)
            if (decompileFile.exists()) {
                FileUtil.delete(decompileFile)
            }
            "java -jar $apktool d -f $apk -o $decompileDir --only-main-classes".command()
        } else {
            print("APK is not exist.")
            false
        }
    }

    /**
     * 替换素材
     */
    abstract fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?)

    /**
     * 替换应用图标
     */
    fun replaceIcon(icon: String): Boolean {
        if (icon.isBlank()) {
            return true
        }
        if (icon.endsWith(".png").not()) {
            print("Icon 格式不正确")
            throw Exception("Icon 格式不正确")
        }
        val iconName = AndroidXmlHandler.getIconName(manifestFile)
        val file = File(icon)

        if (file.exists().not() || file.isFile.not()) {
            print("ICON 路径无效")
            return false
        }

        // xxxhdpi
        DrawableUtil.replaceIcon(decompileDir, file, "xxxhdpi", iconName)

        // xxhdpi 和 drawable
        val xxhdpiImage: File? = DrawableUtil.resizeImage(
            icon,
            144,
            144,
            decompileDir + File.separator + "temp_icon" + File.separator + "xx"
        )
        xxhdpiImage?.let {
            DrawableUtil.replaceIcon(decompileDir, it, "xxhdpi", iconName)
            it.replace(File(decompileDir + File.separator + "res" + File.separator + "drawable" + File.separator + iconName))
        }

        // xhdpi
        val xhdpiImage: File? = DrawableUtil.resizeImage(
            icon,
            96,
            96,
            decompileDir + File.separator + "temp_icon" + File.separator + "xx"
        )
        xhdpiImage?.let {
            DrawableUtil.replaceIcon(decompileDir, it, "xhdpi", iconName)
        }

        val lowDpiImage: File? = DrawableUtil.resizeImage(
            icon,
            72,
            72,
            decompileDir + File.separator + "temp_icon" + File.separator + "xx"
        )
        lowDpiImage?.let {
            DrawableUtil.replaceIcon(decompileDir, it, "hdpi", iconName)
            DrawableUtil.replaceIcon(decompileDir, it, "mdpi", iconName)
            DrawableUtil.replaceIcon(decompileDir, it, "ldpi", iconName)
        }
        return true
    }

    /**
     * 修改 AppName
     */
    fun replaceAppName(appName: String) {

    }

    /**
     * 修改包名
     */
    fun replacePackageName(packageName: String) {

    }

    /**
     * 修改参数配置
     */
    fun updateConfig() {

    }

    /**
     * 注入渠道 SDK
     */
    fun patchChannelSdk() {

    }

    /**
     * 打包
     */
    fun build() {
        // 打包，签名，删除临时目录
    }
}