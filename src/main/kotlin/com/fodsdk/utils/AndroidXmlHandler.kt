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

    fun getRoundIconName(manifestFile: File): String {
        val manifest = manifestFile.readText()
        val pattern = Pattern.compile("android:roundIcon=\"(.*?)\"")
        val matcher = pattern.matcher(manifest)
        matcher.find()
        var iconName = matcher.group(1)
        if (iconName.contains("@drawable/")) {
            iconName = iconName.replace("@drawable/", "")
        } else if (iconName.contains("@mipmap/")) {
            iconName = iconName.replace("@mipmap/", "")
        }
        iconName += ".png"
        println("Round Icon Name: $iconName")
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

    /**
     * v26 Icon 文件修改
     */
    fun redirectV26Icon(decompileDir: String, iconName: String, roundIconName: String = "") {
        val xml = File(
            decompileDir,
            "res" + File.separator + "mipmap-anydpi-v26" + File.separator + iconName.replace(".png", ".xml")
        )
        if (xml.exists()) {
            val document = SAXReader().read(xml)
            document.rootElement.elements().forEach {
                document.rootElement.remove(it)
            }
            val writer = XMLWriter(FileWriter(xml))
            writer.write(document)
            writer.close()

            var xmlContent = xml.readText()
            xmlContent = xmlContent.replace(
                "</adaptive-icon>",
                """
                    <background android:drawable="@mipmap/${iconName.replace(".png", "")}" />
                    <foreground android:drawable="@mipmap/${iconName.replace(".png", "")}" />
                </adaptive-icon>
            """.trimIndent()
            )
            xml.writeText(xmlContent)
        }

        if (roundIconName.isBlank().not()) {
            val roundXml = File(
                decompileDir,
                "res" + File.separator + "mipmap-anydpi-v26" + File.separator + roundIconName.replace(".png", ".xml")
            )
            if (roundXml.exists()) {
                val document = SAXReader().read(roundXml)
                document.rootElement.elements().forEach {
                    document.rootElement.remove(it)
                }
                val writer = XMLWriter(FileWriter(roundXml))
                writer.write(document)
                writer.close()

                var xmlContent = roundXml.readText()
                xmlContent = xmlContent.replace(
                    "</adaptive-icon>",
                    """
                    <background android:drawable="@mipmap/${roundIconName.replace(".png", "")}" />
                    <foreground android:drawable="@mipmap/${roundIconName.replace(".png", "")}" />
                </adaptive-icon>
            """.trimIndent()
                )
                roundXml.writeText(xmlContent)
            }
        }
    }
}