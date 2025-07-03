package com.fodsdk.games

import com.fodsdk.config.FodGameConfig
import com.fodsdk.config.KeyStoreConfig
import com.fodsdk.utils.*
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FilenameFilter
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

abstract class Game(private val apk: String) {

    protected lateinit var decompileDir: String
    private lateinit var apktool: String
    private lateinit var manifestFile: File
    private lateinit var appName: String
    private lateinit var packageName: String
    private lateinit var pkId: String

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
        if (appName.isBlank()) {
            return
        }
        this.appName = appName
        val xml = decompileDir + File.separator + "res" + File.separator + "values" + File.separator + "strings.xml"
        val document = File(xml).loadDocument()
        document?.documentElement?.apply {
            if (hasChildNodes()) {
                val nodes = document.getElementsByTagName("string")
                for (i in 0 until nodes.length) {
                    val node = nodes.item(i)
                    if (node.attributes.getNamedItem("name").nodeValue == "app_name") {
                        node.firstChild.nodeValue = appName
                        break
                    }
                }
            }
        }
        document?.toFile(File(xml))
    }

    /**
     * 修改包名
     */
    fun replacePackageName(packageName: String) {
        this.packageName = packageName
        var manifest = manifestFile.readText()
        val packageMatcher = Pattern.compile("package=\"(.*?)\"").matcher(manifest)
        packageMatcher.find()
        val oldPackageName = packageMatcher.group(1)
        manifest = manifest.replace(oldPackageName, packageName)
        val authoritiesMatcher = Pattern.compile("android:authorities=\"(.*?)\"").matcher(manifest)
        if (authoritiesMatcher.find()) {
            val s = authoritiesMatcher.group(1)
            manifest = manifest.replaceFirst(s, s.replace(packageMatcher.group(1), packageName))
        }

        manifest = manifest.replace("android:name=\"\\.", "android:name=\"$oldPackageName\\.")

        manifestFile.writeText(manifest)
    }

    /**
     * 修改参数配置
     */
    fun updateConfig(sdkVersion: String, pkId: String) {
        this.pkId = pkId
        val file = File(decompileDir + File.separator + "assets" + File.separator + "fod_game_config.json")
        val config = Json.decodeFromString<FodGameConfig>(file.readText())
        config.pkid = pkId
        config.sdkver = sdkVersion
        val json = Json.encodeToString(config)
        file.writeText(json)
    }

    /**
     * 注入渠道 SDK
     */
    fun patchChannelSdk() {
        // TODO
    }

    /**
     * 计算方法数，用于分 Dex
     */
    fun getSmaliMethodNum(smaliFile: File): Int {
        var count = 0
        smaliFile.listFiles { dir, name ->
            File(dir, name).isFile && name.endsWith(".smali")
        }?.forEach {
            val regex = Regex(".method")
            count += regex.findAll(it.readText()).count()
        }
        return count
    }

    /**
     * 打包
     */
    fun build(
        keyStorePath: String,
        generatePath: String,
        appVersion: String,
    ) {
        // 打包，签名，删除临时目录
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("yyMMdd_HHmmss"))
        val fileName = "${appName}_${appVersion}_${gId()}_${pkId}_${time}.apk"
        val filePath = generatePath + File.separator + fileName
        print("文件路径：$filePath")

        if ("java -jar $apktool b $decompileDir".command()) {
            print("回编译成功")
            val unsignedApk = decompileDir + File.separator + "dist" + File.separator + File(apk).name
            val storePassword = KeyStoreConfig.KEY_STORE_PASSWORD
            val keyPassword = KeyStoreConfig.KEY_PASSWORD
            val keyAlias = KeyStoreConfig.KEY_ALIAS
            if ("jarsigner -keystore $keyStorePath -storepass $storePassword -keypass $keyPassword -signedjar $filePath $unsignedApk $keyAlias".command()) {
                print("打包成功")
                FileUtil.delete(File(decompileDir))
            } else {
                print("打包失败")
            }
        } else {
            print("回编译失败")
        }
    }

    abstract fun gId(): Int
}