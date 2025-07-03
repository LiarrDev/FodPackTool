package com.fodsdk.cli

import com.fodsdk.games.GameFactory
import java.io.File

fun main(args: Array<String>) {

    val originApk = args[0]                 // 母包 Apk 路径
    val generatePath = args[1]               // 生成的 Apk 路径
    val apktool = args[3]                   // ApkTool 路径
    val keyStore = args[4]                  // 签名路径

    val gid = args[4]                       // 游戏 GID
    val pkId = args[5]                      // 游戏 PKID
    val appName = args[6]                   // 应用名称
    val packageName = args[7]               // 包名
    val appVersion = args[8]                // 应用版本号
    val sdkVersion = args[9]                // SDK 版本号

    val icon = args[10]                     // ICON 路径
    val loginImg = args[11]                 // 登录背景图路径
    val logoImg = args[12]                  // LOGO 路径
    val loadingImg = args[13]               // 加载背景图路径
    val splashImg = args[14]                // 闪屏路径

    val channelTag = args[15]               // 渠道标记

    println(
        """
         ═════════════════════════════════════════════════════════════════╗
            originApk = $originApk
            generatePath = $generatePath
            apktool = $apktool
            keyStore = $keyStore
            
            gid = $gid
            pkId = $pkId
            appName = $appName
            packageName = $packageName
            appVersion = $appVersion
            sdkVersion = $sdkVersion
            
            icon = $icon
            loginImg = $loginImg
            logoImg = $logoImg
            loadingImg = $loadingImg
            splashImg = $splashImg
            
            channelTag = $channelTag
         ═════════════════════════════════════════════════════════════════╝
    """.trimIndent()
    )

    val decompileDir = generatePath + File.separator + "temp"
    GameFactory(originApk).getGame("")?.run {
        decompile(decompileDir, apktool)
        replaceResource(loginImg, loadingImg, logoImg, splashImg)
        replaceIcon(icon)
        replaceAppName(appName)
        replacePackageName(packageName)
        updateConfig(sdkVersion, pkId)
        patchChannelSdk()
        build(keyStore, generatePath, appVersion)
    }
}
