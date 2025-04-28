package com.fodsdk.utils

import org.w3c.dom.Document
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object FileUtil {
    /**
     * 删除文件或文件夹
     */
    fun delete(file: File) {
        if (file.exists()) {
            if (file.isFile) {
                file.delete()
            } else if (file.isDirectory) {
                val files = file.listFiles()
                files?.forEach {
                    delete(it)
                }
            }
            file.delete()
        } else {
            println("${file.absoluteFile} is not exist")
        }
    }
}

fun File.replace(target: File) {
    if (isFile) {
        copyTo(target, true)
    }
}

fun File.loadDocument(): Document? {
    var document: Document? = null
    try {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        document = builder.parse(this)
        document.normalize()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return document
}

fun Document.toFile(file: File): Boolean {
    return try {
        val source = DOMSource(this)
        val result = StreamResult(file)
        TransformerFactory.newInstance().newTransformer().transform(source, result)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}