package com.github.secretx33.imagetopdf.model

import java.nio.file.Path

data class Settings(
    val files: Set<Path>,
    val combineMode: CombineMode,
    val imageScaleFactor: Double,
    val imageRenderFactor: Double,
)

enum class CombineMode {
    SINGLE_FILE,
    MULTIPLE_FILES,
}