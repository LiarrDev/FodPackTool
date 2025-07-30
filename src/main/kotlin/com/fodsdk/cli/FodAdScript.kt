package com.fodsdk.cli

import com.fodsdk.config.KeyStoreConfig
import com.fodsdk.entity.FodConfig
import com.fodsdk.entity.FodConfigWrapper
import com.fodsdk.utils.*
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

fun main(args: Array<String>) {
    val originApk = args[0]                 // 母包 Apk 路径
    val generatePath = args[1]              // 生成的 Apk 路径
    val apktool = args[2]                   // ApkTool 路径
    val keyStore = args[3]                  // 签名路径
    val zipAlign = args[4]                  // ZipAlign 路径
    val apkSigner = args[5]                 // ApkSigner 路径
    val configPath = args[6]                // 配置路径

    println(
        """
        ═════════════════════════════════════════════════════════════════╗
            originApk = $originApk
            generatePath = $generatePath
            apktool = $apktool
            keyStore = $keyStore
            zipAlign = $zipAlign
            apkSigner = $apkSigner
            configPath = $configPath
         ═════════════════════════════════════════════════════════════════╝
    """.trimIndent()
    )

    val decompileDir = generatePath + File.separator + "temp"
    decompile(originApk, decompileDir, apktool)
    val configWrapper = getConfigWrapper(configPath)
    println("overrideConfig")
    overrideConfig(decompileDir, configWrapper.adParam)
    println("overridePackageName")
    overridePackageName(decompileDir, configWrapper.packageName)
    println("overrideAppName")
    overrideAppName(decompileDir, configWrapper.name)
    println("overrideAppIcon")
    overrideAppIcon(decompileDir, configWrapper.source.icon)
    println("patchChannel")
    packByteDance(decompileDir, configWrapper.patch)
    println("build")
    build(originApk, decompileDir, apktool, zipAlign, apkSigner, keyStore, generatePath, configWrapper)
}

private fun decompile(apk: String, decompileDir: String, apktool: String): Boolean {
    val apkFile = File(apk)
    return if (apkFile.exists() && apkFile.isFile) {
        val decompileFile = File(decompileDir)
        if (decompileFile.exists()) {
            FileUtil.delete(decompileFile)
        }
        "java -jar $apktool d -f $apk -o $decompileDir --only-main-classes".command()
    } else {
        println("APK is not exist.")
        false
    }
}

private fun getConfigWrapper(configPath: String) = Json.decodeFromString<FodConfigWrapper>(File(configPath).readText())

private fun overrideConfig(decompileDir: String, config: FodConfig) {
    val file = File(decompileDir + File.separator + "assets" + File.separator + "fod_game_config.json")
    val sdkVer = Json.decodeFromString<FodConfig>(file.readText()).sdkver
    config.sdkver = sdkVer
    val json = Json.encodeToString(config)
    file.writeText(json)
}

private fun overridePackageName(decompileDir: String, packageName: String) {
    val manifestFile = File(decompileDir, "AndroidManifest.xml")
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

private fun overrideAppName(decompileDir: String, appName: String) {
    if (appName.isBlank()) {
        return
    }
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

private fun overrideAppIcon(decompileDir: String, icon: String): Boolean {
    if (icon.isBlank()) {
        return true
    }
    val manifestFile = File(decompileDir, "AndroidManifest.xml")
    AndroidXmlHandler.removeRoundIcon(manifestFile)
    if (!icon.endsWith(".png")) {
        println("ICON 格式不正确")
        throw Exception("ICON 格式不正确")
    }
    val iconName = AndroidXmlHandler.getIconName(File(decompileDir))
    val file = File(icon)
    return if (file.exists() && file.isFile) {

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

        true
    } else {
        print("ICON 路径无效")
        false
    }
}

fun build(
    apk: String,
    decompileDir: String,
    apktool: String,
    zipAlign: String,
    apkSigner: String,
    keyStorePath: String,
    generatePath: String,
    fodConfigWrapper: FodConfigWrapper
) {
    // 打包，签名，删除临时目录
    val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    val fileName =
        "${fodConfigWrapper.name}_${fodConfigWrapper.adParam.gid}_${fodConfigWrapper.adParam.pkid}_${time}.apk"
    val filePath = generatePath + File.separator + fileName
    println("文件路径：$filePath")

    if ("java -jar $apktool b $decompileDir".command()) {
        println("回编译成功")
        val unsignedApk = decompileDir + File.separator + "dist" + File.separator + File(apk).name
        val alignApk = decompileDir + File.separator + "dist" + File.separator + "align.apk"
        if ("$zipAlign 4 $unsignedApk $alignApk".command()) {
            println("对齐完成")
        } else {
            println("对齐失败")
            throw RuntimeException("对齐失败")
        }

        val storePassword = KeyStoreConfig.KEY_STORE_PASSWORD
        val keyPassword = KeyStoreConfig.KEY_PASSWORD
        val keyAlias = KeyStoreConfig.KEY_ALIAS
        if ("java -jar $apkSigner sign --ks $keyStorePath --ks-key-alias $keyAlias --ks-pass pass:$storePassword --key-pass pass:$keyPassword --out $filePath $alignApk".command()) {
            println("签名完成")
            FileUtil.delete(File(decompileDir))
            println("打包成功")
        } else {
            println("签名失败")
            throw RuntimeException("签名失败")
        }
    } else {
        println("回编译失败")
    }
}

fun packByteDance(decompileDir: String, patchDir: String) {
    ByteDanceHandler.handleAndroidManifest(decompileDir)
    ByteDanceHandler.patchChannelFile(decompileDir, patchDir)
}