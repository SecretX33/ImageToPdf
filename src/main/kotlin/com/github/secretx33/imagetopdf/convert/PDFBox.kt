package com.github.secretx33.imagetopdf.convert

import com.github.secretx33.imagetopdf.exitWithMessage
import com.github.secretx33.imagetopdf.model.Settings
import org.apache.pdfbox.pdfwriter.compress.CompressParameters
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.awt.RenderingHints.*
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import javax.imageio.ImageIO
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.outputStream

fun createPdf(file: Path, block: PDDocument.() -> Unit) = try {
    PDDocument().use { document ->
        document.block()
        file.outputStream(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).buffered().use {
            document.save(it, CompressParameters.DEFAULT_COMPRESSION)
        }
    }
} catch (e: Exception) {
    exitWithMessage("Error creating file ${file.absolutePathString()}.\n${e.stackTraceToString()}")
}

fun PDDocument.addImage(
    picture: Path,
    settings: Settings
) {
    val image = PDImageXObject.createFromFile(picture.absolutePathString(), this)
        .scale(settings.imageScaleFactor, this, picture)

    val scaled = ScalableDimension(image.width, image.height, settings.imageRenderFactor).scale()
    val page = PDPage(PDRectangle(scaled.getWidth().toFloat(), scaled.getHeight().toFloat()))
        .also(::addPage)

    PDPageContentStream(this, page).use { contentStream ->
        contentStream.drawImage(image, 0f, 0f, scaled.getWidth().toFloat(), scaled.getHeight().toFloat())
    }
}


fun PDImageXObject.scale(factor: Double, document: PDDocument, file: Path): PDImageXObject {
    if (factor == 1.0) {
        return this
    }
    require(factor > 0) { "Invalid factor value (expected > 0, actual: $factor)" }

    val original = ScalableDimension(width, height, factor)
    val dimensions = DimensionTuple(original, original.scale())
    val scaled = dimensions.modified
    val widthScaled = scaled.getWidth().toInt()
    val heightScaled = scaled.getHeight().toInt()

    val resizedImage = BufferedImage(widthScaled, heightScaled, BufferedImage.TYPE_INT_RGB)
    val graphics = resizedImage.createGraphics().apply {
        setRenderingHints(RENDERING_HINTS)
        transform = transform.apply {
            setToScale(dimensions.widthRatio, dimensions.heightRatio)
        }
    }

    try {
        graphics.drawImage(image, 0, 0, null)
    } finally {
        graphics.dispose()
    }

    return resizedImage.toPDImageXObject(document, file)
}

fun BufferedImage.toPDImageXObject(document: PDDocument, file: Path): PDImageXObject {
    val scaledImageBytes = ByteArrayOutputStream().use {
        ImageIO.write(this, file.extension, it)
        it.toByteArray()
    }
    return PDImageXObject.createFromByteArray(document, scaledImageBytes, file.name)
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
