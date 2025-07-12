package com.github.secretx33.imagetopdf.model

import com.github.secretx33.imagetopdf.util.sizeInBytes
import org.apache.pdfbox.pdmodel.PDDocument
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.nio.file.Path
import kotlin.io.path.extension

data class PdfImage(
    val image: BufferedImage,
    val fileName: Path,  // Only the name and extension
    val file: Path,      // The full path to the original image
    val width: Int,
    val height: Int,
    val document: PDDocument,
    val mirroring: ImageMirroring,
    val rotation: ImageRotation,
) {
    val dimension = Dimension(width, height)
    val sizeInBytes by lazy { image.sizeInBytes(fileName.extension) }
}