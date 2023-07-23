package com.github.secretx33.imagetopdf.model

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.nio.file.Path

data class LazyImage(
    val lazyImage: Lazy<PDImageXObject>,
    val fileName: Path,  // Only the name and extension
    val file: Path,      // The full path to the original image
    val width: Int,
    val height: Int,
    val document: PDDocument,
) {
    val image by lazyImage
}