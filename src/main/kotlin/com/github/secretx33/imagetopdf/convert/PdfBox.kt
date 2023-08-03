package com.github.secretx33.imagetopdf.convert

import com.github.secretx33.imagetopdf.model.LazyImage
import com.github.secretx33.imagetopdf.model.Settings
import com.github.secretx33.imagetopdf.util.ANSI_RESET
import com.github.secretx33.imagetopdf.util.ANSI_YELLOW
import com.github.secretx33.imagetopdf.util.absoluteParent
import com.github.secretx33.imagetopdf.util.bail
import com.github.secretx33.imagetopdf.util.byteArrayOutputStream
import com.github.secretx33.imagetopdf.util.formattedFileSize
import com.github.secretx33.imagetopdf.util.lazyNone
import org.apache.pdfbox.pdfwriter.compress.CompressParameters
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints.KEY_ALPHA_INTERPOLATION
import java.awt.RenderingHints.KEY_ANTIALIASING
import java.awt.RenderingHints.KEY_COLOR_RENDERING
import java.awt.RenderingHints.KEY_DITHERING
import java.awt.RenderingHints.KEY_FRACTIONALMETRICS
import java.awt.RenderingHints.KEY_INTERPOLATION
import java.awt.RenderingHints.KEY_RENDERING
import java.awt.RenderingHints.KEY_STROKE_CONTROL
import java.awt.RenderingHints.KEY_TEXT_ANTIALIASING
import java.awt.RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
import java.awt.RenderingHints.VALUE_ANTIALIAS_ON
import java.awt.RenderingHints.VALUE_COLOR_RENDER_QUALITY
import java.awt.RenderingHints.VALUE_DITHER_DISABLE
import java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON
import java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC
import java.awt.RenderingHints.VALUE_RENDER_QUALITY
import java.awt.RenderingHints.VALUE_STROKE_PURE
import java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON
import java.awt.image.BufferedImage
import java.awt.image.RenderedImage
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.MemoryCacheImageOutputStream
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.outputStream

fun createPdf(file: Path, block: PDDocument.() -> Unit) = try {
    PDDocument().use { document ->
        document.block()
        if (file.isRegularFile()) {
            println("${ANSI_YELLOW}Warn: file '${file.name}' at '${file.absoluteParent}' already exists, overriding it (${file.formattedFileSize()})$ANSI_RESET")
        }
        file.outputStream(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).buffered().use {
            document.save(it, CompressParameters.DEFAULT_COMPRESSION)
        }
    }
} catch (e: Exception) {
    bail("Error creating file ${file.absolutePathString()}.\n${e.stackTraceToString()}")
}

fun PDDocument.addImage(
    picture: Path,
    settings: Settings,
) {
    val lazyImage = lazyImage(picture)
        .resize(settings.imageResizeFactor)
        .compressToJpg(settings.jpgCompressionQuality)

    val scaled = ScalableDimension(lazyImage.width, lazyImage.height, settings.imageRenderFactor).scale()
    val page = PDPage(PDRectangle(scaled.getWidth().toFloat(), scaled.getHeight().toFloat()))
        .also(::addPage)

    PDPageContentStream(this, page).use { contentStream ->
        contentStream.drawImage(lazyImage.image, 0f, 0f, scaled.getWidth().toFloat(), scaled.getHeight().toFloat())
    }
}

private fun PDDocument.lazyImage(picture: Path): LazyImage {
    val image = PDImageXObject.createFromFile(picture.absolutePathString(), this)
    return LazyImage(
        lazyImage = lazyNone { image },
        fileName = picture.fileName,
        file = picture,
        width = image.width,
        height = image.height,
        document = this,
    )
}

private fun LazyImage.resize(factor: Double): LazyImage {
    if (factor == 1.0) {
        return this
    }
    require(factor > 0) { "Invalid factor value (expected > 0, actual: $factor)" }

    val dimensions = ScalableDimension(width, height, factor).toScaledDimensionTuple()
        .let {
            it.copy(modified = ScalableDimension(
                it.modified.getWidth().coerceAtLeast(1.0),
                it.modified.getHeight().coerceAtLeast(1.0),
            ))
        }

    return copy(
        lazyImage = lazyNone { image.resize(dimensions, document, fileName) },
        width = dimensions.modified.getWidth().toInt(),
        height = dimensions.modified.getHeight().toInt(),
    )
}

private fun PDImageXObject.resize(
    dimensionTuple: DimensionTuple,
    document: PDDocument,
    fileName: Path,
): PDImageXObject {
    val widthScaled = dimensionTuple.modified.getWidth().toInt()
    val heightScaled = dimensionTuple.modified.getHeight().toInt()

    val resizedImage = BufferedImage(widthScaled, heightScaled, image.colorModel.hasAlpha()).graphics {
        setRenderingHints(RENDERING_HINTS)
        transform = transform.apply {
            setToScale(dimensionTuple.widthRatio, dimensionTuple.heightRatio)
        }
        drawImage(image, 0, 0, null)
    }

    return resizedImage.toPDImageXObject(document, fileName)
}

private fun LazyImage.compressToJpg(jpgCompressionQuality: Double?): LazyImage {
    if (jpgCompressionQuality == null) {
        return this
    }
    require(jpgCompressionQuality in 0.0..1.0) { "JPG compression factor must be between 0 and 1 (actual: $jpgCompressionQuality)" }

    val newFileName = Path("${fileName.nameWithoutExtension}.jpg")
    return copy(
        lazyImage = lazyNone {
            image.convertToJpg(document, fileName)
                .setJpgQuality(jpgCompressionQuality, document, newFileName)
        },
        fileName = newFileName,
    )
}

private fun PDImageXObject.convertToJpg(document: PDDocument, fileName: Path): PDImageXObject {
    if (fileName.extension.lowercase() in setOf("jpg", "jpeg")) {
        return this
    }

    val outputImage = BufferedImage(width, height, false).graphics {
        drawImage(image, 0, 0, Color.WHITE, null)
    }
    val imageBytes = outputImage.toByteArray("jpg")

    return PDImageXObject.createFromByteArray(document, imageBytes, "${fileName.nameWithoutExtension}.jpg")
}

private fun BufferedImage.removeAlphaChannel(): BufferedImage {
    if (!colorModel.hasAlpha()) {
        return this
    }
    val target = BufferedImage(width, height, false).graphics {
        fillRect(0, 0, width, height)
        drawImage(this@removeAlphaChannel, 0, 0, null)
    }
    return target
}

private fun PDImageXObject.setJpgQuality(
    jpgCompressFactor: Double,
    document: PDDocument,
    fileName: Path,
): PDImageXObject {
    val jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next()
    try {
        val jpgWriteParam = jpgWriter.defaultWriteParam.apply {
            compressionMode = ImageWriteParam.MODE_EXPLICIT
            compressionQuality = jpgCompressFactor.toFloat()
        }

        val imageBytes = byteArrayOutputStream { baos ->
            MemoryCacheImageOutputStream(baos).use {
                jpgWriter.output = it
                val outputImage = IIOImage(image, null, null)
                jpgWriter.write(null, outputImage, jpgWriteParam)
            }
        }
        return PDImageXObject.createFromByteArray(document, imageBytes, fileName.name)
    } finally {
        jpgWriter.dispose()
    }
}

private fun BufferedImage.toPDImageXObject(
    document: PDDocument,
    fileName: Path,
): PDImageXObject = PDImageXObject.createFromByteArray(document, toByteArray(fileName.extension), fileName.name)

private fun <T> BufferedImage.graphics(block: Graphics2D.() -> T): BufferedImage = apply {
    val graphics = createGraphics()
    try {
        graphics.block()
    } finally {
        graphics.dispose()
    }
}

private fun BufferedImage(
    width: Int,
    height: Int,
    hasAlpha: Boolean,
): BufferedImage = BufferedImage(
    width,
    height,
    if (hasAlpha) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB,
)

private fun RenderedImage.toByteArray(fileExtension: String): ByteArray = byteArrayOutputStream {
    ImageIO.write(this, fileExtension, it)
}

private val RENDERING_HINTS = mapOf(
    KEY_ANTIALIASING to VALUE_ANTIALIAS_ON,
    KEY_ALPHA_INTERPOLATION to VALUE_ALPHA_INTERPOLATION_QUALITY,
    KEY_COLOR_RENDERING to VALUE_COLOR_RENDER_QUALITY,
    KEY_DITHERING to VALUE_DITHER_DISABLE,
    KEY_FRACTIONALMETRICS to VALUE_FRACTIONALMETRICS_ON,
    KEY_INTERPOLATION to VALUE_INTERPOLATION_BICUBIC,
    KEY_RENDERING to VALUE_RENDER_QUALITY,
    KEY_STROKE_CONTROL to VALUE_STROKE_PURE,
    KEY_TEXT_ANTIALIASING to VALUE_TEXT_ANTIALIAS_ON,
)
