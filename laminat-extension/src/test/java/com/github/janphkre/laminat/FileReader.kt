package com.github.janphkre.laminat

import org.apache.http.Consts
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

fun readFile(file: File): String {
    val text = StringBuilder()
    try {
        val br = BufferedReader(InputStreamReader(file.inputStream(), Consts.UTF_8.name()))
        var line: String?

        while (true) {
            line = br.readLine()
            if (line == null) {
                break
            }
            text.append(line)
            text.append('\n')
        }
        br.close()
    } catch (e: Exception) {
        System.err.println("readMockFile: failed to read ${file.name}")
        e.printStackTrace()
    }

    return text.toString()
}