package com.github.secretx33.imagetopdf.model

import java.nio.file.Path

data class Settings(
    val files: Set<Path>,
    val combineMode: CombineMode,
    val imageResizeFactor: Double,
    val imageRenderFactor: Double,
    val jpgCompressionQuality: Double?,
    val sortFilesBy: SortFilesBy?,
    val isInteractive: Boolean,
)

fun CliParams.toSettings(): Settings {
    val fileSet = files.toSet()
    return Settings(
        files = fileSet,
        combineMode = if (fileSet.size >= 2 && !isCombine) CombineMode.MULTIPLE_FILES else CombineMode.SINGLE_FILE,
        imageResizeFactor = imageResizeFactor,
        imageRenderFactor = imageRenderFactor,
        jpgCompressionQuality = jpgCompressionQuality,
        sortFilesBy = sortFilesBy,
        isInteractive = isInteractive,
    )
}

enum class CombineMode(val displayName: String) {
    SINGLE_FILE("Single file"),
    MULTIPLE_FILES("Multiple files");
}