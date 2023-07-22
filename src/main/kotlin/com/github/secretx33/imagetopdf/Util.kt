package com.github.secretx33.imagetopdf

import com.github.secretx33.imagetopdf.exception.QuitApplicationException
import java.lang.invoke.MethodHandles
import java.nio.file.Path
import java.util.Locale
import kotlin.io.path.fileSize

private val thisClass = MethodHandles.lookup().lookupClass()

fun printError(message: String) = System.err.println(message)

fun exitWithMessage(message: String): Nothing = throw QuitApplicationException(message)

fun Path.formattedFileSize(): String = fileSize().bytesToHumanReadableSize()

fun getTextResource(path: String): String = thisClass.classLoader.getResourceAsStream(path)
    ?.bufferedReader()
    ?.use { it.readText() }
    ?: throw IllegalArgumentException("$path was not found in classpath")

private fun Long.bytesToHumanReadableSize(): String = when {
    this == Long.MIN_VALUE || this < 0 -> "N/A"
    this < 1024L -> "$this bytes"
    this <= 0xfffccccccccccccL shr 40 -> "%.1f KB".format(Locale.ROOT, toDouble() / (0x1 shl 10))
    this <= 0xfffccccccccccccL shr 30 -> "%.1f MB".format(Locale.ROOT, toDouble() / (0x1 shl 20))
    this <= 0xfffccccccccccccL shr 20 -> "%.1f GB".format(Locale.ROOT, toDouble() / (0x1 shl 30))
    this <= 0xfffccccccccccccL shr 10 -> "%.1f TB".format(Locale.ROOT, toDouble() / (0x1 shl 40))
    this <= 0xfffccccccccccccL -> "%.1f PB".format(Locale.ROOT, (this shr 10).toDouble() / (0x1 shl 40))
    else -> "%.1f EB".format(Locale.ROOT, (this shr 20).toDouble() / (0x1 shl 40))
}
