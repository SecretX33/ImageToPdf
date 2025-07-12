package com.github.secretx33.imagetopdf.util

import com.drew.metadata.Metadata
import com.drew.metadata.exif.ExifIFD0Directory
import com.github.secretx33.imagetopdf.exception.QuitApplicationException
import com.github.secretx33.imagetopdf.model.ImageMirroring
import com.github.secretx33.imagetopdf.model.ImageRotation
import org.jnativehook.GlobalScreen
import java.awt.image.RenderedImage
import java.io.ByteArrayOutputStream
import java.lang.invoke.MethodHandles
import java.nio.file.Path
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.fileSize
import kotlin.time.Duration

private val thisClass = MethodHandles.lookup().lookupClass()

val cpuThreadAmount: Int get() = Runtime.getRuntime().availableProcessors()

fun printError(message: String) = System.err.println(message)

fun bail(message: String): Nothing = throw QuitApplicationException(message, exitCode = 1)

fun quitProgram(): Nothing = throw QuitApplicationException(exitCode = 0)

val Path.absoluteParent: Path get() = (parent ?: Path("")).absolute()

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

/**
 * Returns a string representation of the duration in seconds, with increased precision when the time is below one
 * second.
 *
 * @return a formatted string representation of the duration in seconds.
 */
fun Duration.formattedSeconds(): String {
    val secondsDouble = inWholeMilliseconds.toDouble() / 1000.0
    val pattern = when {
        inWholeSeconds <= 0 -> "#.##"
        else -> "#,###.#"
    }
    val format = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
    return format.format(secondsDouble)
}

fun Double.formattedDecimal(digits: Int = 1): String {
    val pattern = "#${if (digits > 0) ".${"#".repeat(digits)}" else ""}"
    val format = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
    return format.format(this)
}

/**
 * Bye bye, annoying `info` logger from JNativeHook.
 */
fun disableAnnoyingJnativehookLogger() {
    // Get the logger for "org.jnativehook" and set the level to warning.
    val logger = Logger.getLogger(GlobalScreen::class.java.getPackage().name)
    logger.level = Level.WARNING

    // Don't forget to disable the parent handlers.
    logger.useParentHandlers = false
}

fun byteArrayOutputStream(block: (ByteArrayOutputStream) -> Unit): ByteArray = ByteArrayOutputStream().use {
    block(it)
    it.toByteArray()
}

fun RenderedImage.toByteArray(fileExtension: String): ByteArray = byteArrayOutputStream {
    ImageIO.write(this, fileExtension, it)
}

fun RenderedImage.sizeInBytes(fileExtension: String): Long = CountingOutputStream().use {
    ImageIO.write(this, fileExtension, it)
    it.byteCount
}

val Metadata.imageRotation: ImageRotation get() {
    val exif = getFirstDirectoryOfType(ExifIFD0Directory::class.java) ?: return ImageRotation.NONE
    val orientation = exif.getInt(ExifIFD0Directory.TAG_ORIENTATION)
    return ImageRotation.from(orientation)
}

val Metadata.imageMirroring: ImageMirroring get() {
    val exif = getFirstDirectoryOfType(ExifIFD0Directory::class.java) ?: return ImageMirroring.NONE
    val orientation = exif.getInt(ExifIFD0Directory.TAG_ORIENTATION)
    return ImageMirroring.from(orientation)
}