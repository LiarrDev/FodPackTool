package com.fodsdk.com.fodsdk.utils

import java.io.File

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