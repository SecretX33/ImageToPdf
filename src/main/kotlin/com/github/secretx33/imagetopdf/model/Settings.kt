package com.github.secretx33.imagetopdf.model

import java.nio.file.Path

data class Settings(
    val files: Set<Path>,
    val combineMode: CombineMode,
    val imageScaleFactor: Double,
    val imageRenderFactor: Double,
    val jpgCompressionQuality: Double?,
    val sortFilesBy: SortFilesBy?,
    val isInteractive: Boolean,
)

fun CliParams.toSettings(): Settings {
    val fileSet = files.toSet()
    return Settings(
        files = fileSet,
        combineMode = if (fileSet.size >= 2 && !willCombine) CombineMode.MULTIPLE_FILES else CombineMode.SINGLE_FILE,
        imageScaleFactor = imageResizeFactor,
        imageRenderFactor = imageRenderFactor,
        jpgCompressionQuality = jpgCompressionQuality,
        sortFilesBy = sortFilesBy,
        isInteractive = isInteractive,
    )
}

enum class CombineMode {
    SINGLE_FILE,
    MULTIPLE_FILES;

    val displayName = name.replace("_", " ")
}