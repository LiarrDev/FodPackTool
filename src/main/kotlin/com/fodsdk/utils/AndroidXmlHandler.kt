package com.fodsdk.utils

import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import java.io.File
import java.io.FileWriter
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

    /**
     * 移除 android:roundIcon 属性
     */
    fun removeRoundIcon(androidManifest: File) {
        val document = SAXReader().read(androidManifest)
        val application = document.rootElement.element("application")
        val attr = application.attribute("roundIcon") ?: return
        application.remove(attr)
        val writer = XMLWriter(FileWriter(androidManifest))
        writer.write(document)
        writer.close()
        println("移除 android:roundIcon 属性")
    }
}