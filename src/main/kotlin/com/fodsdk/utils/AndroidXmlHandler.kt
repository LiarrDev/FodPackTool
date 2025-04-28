package com.fodsdk.utils

import java.io.File
import java.util.regex.Pattern

object AndroidXmlHandler {

    /**
     * 获取 ICON 文件名
     */
    fun getIconName(manifestFile: File): String {
        val manifest = manifestFile.readText()
        val pattern = Pattern.compile("android:icon=\"(.*?)\"")
        val matcher = pattern.matcher(manifest)
        matcher.find()
        var iconName = matcher.group(1)
        if (iconName.contains("@drawable/")) {
            iconName = iconName.replace("@drawable/", "")
        } else if (iconName.contains("@mipmap/")) {
            iconName = iconName.replace("@mipmap/", "")
        }
        iconName += ".png"
        println("Icon Name: $iconName")
        return iconName
    }
}