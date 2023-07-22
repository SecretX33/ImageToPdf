package com.github.secretx33.imagetopdf

import com.github.secretx33.imagetopdf.model.CombineMode
import com.github.secretx33.imagetopdf.model.Settings
import java.nio.file.Path
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.notExists

fun fetchSettings(args: Array<String>): Settings {
    val files = args.mapTo(mutableSetOf(), ::Path)
    validatePaths(files)
    return getSettings(files)
}

private fun validatePaths(paths: Set<Path>): Set<Path> {
    var errorMessage: String? = null

    if (paths.isEmpty()) {
        errorMessage = "Invalid argument: this program requires at least one file passed as argument to function."
    }
    paths.onEach {
        errorMessage = errorMessage ?: when {
            it.notExists() -> "Invalid argument: file '$it' does not seems to exist."
            !it.isSupportedFormat() -> "Invalid argument: file '$it' is of an illegal type '${it.extension}', this program only support these extensions: ${supportedExtensions.joinToString(", ") { ".$it" }}."
            !it.isRegularFile() -> "Invalid argument: '$it' is not a file (maybe it is a folder?)"
            else -> null
        }
    }
    errorMessage?.let(::exitWithMessage)
    return paths
}

private val supportedExtensions = setOf("jpg", "jpeg", "png")

private fun Path.isSupportedFormat(): Boolean = supportedExtensions.any { extension.equals(it, ignoreCase = true) }

private fun getSettings(files: Set<Path>): Settings {
    val index = AtomicInteger(1)

    val combineMode = if (files.size >= 2) {
        print("${index.getAndIncrement()}. You dragged more than one file, do you want to combine them into a single PDF file (y/n) (default: y)? ")
        if (readBool(true)) CombineMode.SINGLE_FILE else CombineMode.MULTIPLE_FILES
    } else {
        CombineMode.SINGLE_FILE
    }

    print("${index.getAndIncrement()}. Reduce the image resolution by this factor (default: 1.0 -> do not reduce): ")
    val scaleFactor = readDouble(1.0)

    print("${index.getAndIncrement()}. Choose the image scale factor in the PDF, the smaller the value, the greater the image clarity (default: 0.5): ")
    val renderFactor = readDouble(0.5)

    return Settings(
        files = files,
        combineMode = combineMode,
        imageScaleFactor = scaleFactor,
        imageRenderFactor = renderFactor,
    )
}

private fun readString(): String = readln()

private fun readDouble(default: Double? = null): Double {
    var value: Double? = null
    while (value == null) {
        val valueAsString = readString()
        if (default != null && valueAsString.isBlank()) {
            println(default)
            return default
        }

        value = valueAsString.toDoubleOrNull()?.takeIf { it.isFinite() }

        if (value == null) {
            printError("Invalid number, try again.")
        }
    }
    return value
}

private fun readBool(default: Boolean? = null): Boolean {
    var value: Boolean? = null
    while (value == null) {
        val valueAsString = readString().lowercase(Locale.US)
        if (default != null && valueAsString.isBlank()) {
            println(if (default) "y" else "n")
            return default
        }

        value = when (valueAsString) {
            "y", "yes", "true", "1" -> true
            "n", "no", "false", "0" -> false
            else -> null
        }

        if (value == null) {
            printError("Invalid option, try again.")
        }
    }
    return value
}