package com.fodsdk.cli

import com.fodsdk.config.KeyStoreConfig
import com.fodsdk.entity.FodConfig
import com.fodsdk.entity.FodConfigWrapper
import com.fodsdk.utils.FileUtil
import com.fodsdk.utils.command
import com.fodsdk.utils.loadDocument
import com.fodsdk.utils.toFile
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
    val configPath = args[4]                // 配置路径

    println(
        """
        ═════════════════════════════════════════════════════════════════╗
            originApk = $originApk
            generatePath = $generatePath
            apktool = $apktool
            keyStore = $keyStore
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
    println("build")
    build(originApk, decompileDir, apktool, keyStore, generatePath, configWrapper)
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

fun build(
    apk: String,
    decompileDir: String,
    apktool: String,
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
        val storePassword = KeyStoreConfig.KEY_STORE_PASSWORD
        val keyPassword = KeyStoreConfig.KEY_PASSWORD
        val keyAlias = KeyStoreConfig.KEY_ALIAS
        if ("jarsigner -keystore $keyStorePath -storepass $storePassword -keypass $keyPassword -signedjar $filePath $unsignedApk $keyAlias".command()) {
            println("打包成功")
            FileUtil.delete(File(decompileDir))
        } else {
            println("打包失败")
        }
    } else {
        println("回编译失败")
    }
}