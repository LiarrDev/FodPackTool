package com.fodsdk.utils

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter

/**
 * 用于处理 Runtime.getRuntime().exec() 产生的错误流及输出流
 */
class StreamGobbler(
    private val inputStream: InputStream,
    private val type: String,
    private val outputStream: OutputStream? = null
) : Thread() {

    override fun run() {
        try {
            var printWriter: PrintWriter? = null
            outputStream?.let {
                printWriter = PrintWriter(it)
            }
            val inputStreamReader = InputStreamReader(inputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                printWriter?.println(line)
                println("$type > $line")
            }
            printWriter?.flush()
            printWriter?.close()
            bufferedReader.close()
            inputStreamReader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}