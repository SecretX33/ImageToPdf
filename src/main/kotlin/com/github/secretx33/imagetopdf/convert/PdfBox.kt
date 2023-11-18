package com.github.secretx33.imagetopdf.convert

import arrow.core.nonFatalOrThrow
import com.drew.imaging.ImageMetadataReader
import com.github.secretx33.imagetopdf.model.ImageMirroring
import com.github.secretx33.imagetopdf.model.ImageRotation
import com.github.secretx33.imagetopdf.model.PdfImage
import com.github.secretx33.imagetopdf.model.Settings
import com.github.secretx33.imagetopdf.util.ANSI_RESET
import com.github.secretx33.imagetopdf.util.ANSI_YELLOW
import com.github.secretx33.imagetopdf.util.absoluteParent
import com.github.secretx33.imagetopdf.util.bail
import com.github.secretx33.imagetopdf.util.byteArrayOutputStream
import com.github.secretx33.imagetopdf.util.formattedFileSize
import com.github.secretx33.imagetopdf.util.imageMirroring
import com.github.secretx33.imagetopdf.util.imageRotation
import org.apache.pdfbox.pdfwriter.compress.CompressParameters
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.awt.Color
import java.awt.Dimension
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
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
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

inline fun createPdf(file: Path, block: PDDocument.() -> Unit) = try {
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
    bail("Error creating file ${file.absolutePathString()}.\n${e.nonFatalOrThrow().stackTraceToString()}")
}

fun PDDocument.createPdfImage(picture: Path, settings: Settings): PdfImage = pdfImage(picture)
    .mirrorAndRotate()
    .resize(settings.imageResizeFactor)
    .compressToJpg(settings.jpgCompressionQuality)

fun PDDocument.addImage(
    pdfImage: PdfImage,
    settings: Settings,
) {
    val scaled = TransformableDimension(pdfImage.width, pdfImage.height, settings.imageRenderFactor).scale()
    val page = PDPage(PDRectangle(scaled.getWidth().toFloat(), scaled.getHeight().toFloat()))
        .also(::addPage)
    val image = pdfImage.image.toPDImageXObject(this, pdfImage.fileName)

    PDPageContentStream(this, page).use { contentStream ->
        contentStream.drawImage(image, 0f, 0f, scaled.getWidth().toFloat(), scaled.getHeight().toFloat())
    }
}

private fun PDDocument.pdfImage(picture: Path): PdfImage {
    val image = ImageIO.read(picture.toFile())
    val metadata = runCatching { ImageMetadataReader.readMetadata(picture.toFile()) }

    val imageMirror = metadata.mapCatching { it.imageMirroring }
        .onFailure { println("${ANSI_YELLOW}Warn: could not read image mirroring of file '${picture.name}' at '${picture.absoluteParent}', assuming no mirroring metadata is set$ANSI_RESET") }
    val imageRotate = metadata.mapCatching { it.imageRotation }
        .onFailure { println("${ANSI_YELLOW}Warn: could not read image orientation of file '${picture.name}' at '${picture.absoluteParent}', assuming no rotation metadata is set$ANSI_RESET") }

    return PdfImage(
        image = image,
        fileName = picture.fileName,
        file = picture,
        width = image.width,
        height = image.height,
        document = this,
        mirroring = imageMirror.getOrElse { ImageMirroring.NONE },
        rotation = imageRotate.getOrElse { ImageRotation.NONE },
    )
}

private fun PdfImage.mirrorAndRotate(): PdfImage {
    if (rotation == ImageRotation.NONE && mirroring == ImageMirroring.NONE) {
        return this
    }
    val image = image.mirrorAndRotate(rotation, mirroring)
    return copy(
        image = image,
        width = image.width,
        height = image.height,
    )
}

private fun BufferedImage.mirrorAndRotate(
    rotation: ImageRotation,
    mirroring: ImageMirroring,
): BufferedImage {
    val dimension = TransformableDimension(width, height)
    val rotatedDimension = dimension.rotate(rotation.degrees)
    val affineTransforms = listOfNotNull(
        mirrorAffineTransform(mirroring, dimension),
        rotateAffineTransform(rotation, dimension),
    ).map {
        AffineTransformOp(it, null)
    }

    val rotatedImage = BufferedImage(rotatedDimension.width, rotatedDimension.height, colorModel.hasAlpha()) {
        val transformedImage = affineTransforms.fold(this@mirrorAndRotate) { image, transformOp ->
            transformOp.filter(image, null)
        }
        drawImage(transformedImage, 0, 0, null)
    }
    return rotatedImage
}

private fun rotateAffineTransform(
    rotation: ImageRotation,
    dimension: TransformableDimension,
): AffineTransform? {
    if (rotation == ImageRotation.NONE) {
        return null
    }
    val rotatedDimension = dimension.rotate(rotation.degrees)
    return AffineTransform().apply {
        translate((rotatedDimension.getWidth() - dimension.getWidth()) / 2.0, (rotatedDimension.getHeight() - dimension.getHeight()) / 2.0)
        rotate(Math.toRadians(rotation.degrees), dimension.getWidth() / 2.0, dimension.getHeight() / 2.0)
    }
}

private fun mirrorAffineTransform(
    mirroring: ImageMirroring,
    dimension: Dimension,
): AffineTransform? {
    if (mirroring == ImageMirroring.NONE) {
        return null
    }
    return AffineTransform().apply {
        if (mirroring == ImageMirroring.HORIZONTAL) {
            scale(-1.0, 1.0)
            translate(-dimension.getWidth(), 0.0)
        } else {
            scale(1.0, -1.0)
            translate(0.0, -dimension.getHeight())
        }
    }
}

private fun PdfImage.resize(factor: Double): PdfImage {
    if (factor == 1.0) {
        return this
    }
    require(factor > 0) { "Invalid factor value (expected > 0, actual: $factor)" }

    val dimensions = TransformableDimension(width, height, factor).toScaledDimensionTuple()
        .let {
            it.copy(modified = TransformableDimension(
                it.modified.getWidth().coerceAtLeast(1.0),
                it.modified.getHeight().coerceAtLeast(1.0),
            ))
        }

    return copy(
        image = image.resize(dimensions),
        width = dimensions.modified.getWidth().toInt(),
        height = dimensions.modified.getHeight().toInt(),
    )
}

private fun BufferedImage.resize(dimensionTuple: DimensionTuple): BufferedImage {
    val widthScaled = dimensionTuple.modified.getWidth().toInt()
    val heightScaled = dimensionTuple.modified.getHeight().toInt()

    val resizedImage = BufferedImage(widthScaled, heightScaled, colorModel.hasAlpha()) {
        setRenderingHints(RENDERING_HINTS)
        transform = transform.apply {
            setToScale(dimensionTuple.widthRatio, dimensionTuple.heightRatio)
        }
        drawImage(this@resize, 0, 0, null)
    }
    return resizedImage
}

private fun PdfImage.compressToJpg(jpgCompressionQuality: Double?): PdfImage {
    if (jpgCompressionQuality == null) {
        return this
    }
    require(jpgCompressionQuality in 0.0..1.0) { "JPG compression factor must be between 0 and 1 (actual: $jpgCompressionQuality)" }

    val jpgImage = image.convertToJpg(dimension, fileName)
        .setJpgQuality(jpgCompressionQuality)
    val newFileName = Path("${fileName.nameWithoutExtension}.jpg")

    return copy(
        image = jpgImage,
        fileName = newFileName,
    )
}

private fun BufferedImage.convertToJpg(dimension: Dimension, fileName: Path): BufferedImage {
    if (fileName.extension.lowercase() in setOf("jpg", "jpeg")) {
        return this
    }
    val outputImage = BufferedImage(dimension.width, dimension.height, false) {
        drawImage(this@convertToJpg, 0, 0, Color.WHITE, null)
    }
    return outputImage
}

private fun BufferedImage.setJpgQuality(jpgCompressFactor: Double): BufferedImage {
    val jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next()
    try {
        val jpgWriteParam = jpgWriter.defaultWriteParam.apply {
            compressionMode = ImageWriteParam.MODE_EXPLICIT
            compressionQuality = jpgCompressFactor.toFloat()
        }

        val imageBytes = byteArrayOutputStream { outputStream ->
            MemoryCacheImageOutputStream(outputStream).use {
                jpgWriter.output = it
                val outputImage = IIOImage(this, null, null)
                jpgWriter.write(null, outputImage, jpgWriteParam)
            }
        }
        return imageBytes.inputStream().use(ImageIO::read)
    } finally {
        jpgWriter.dispose()
    }
}

private fun BufferedImage.toPDImageXObject(
    document: PDDocument,
    fileName: Path,
): PDImageXObject = PDImageXObject.createFromByteArray(document, toByteArray(fileName.extension), fileName.name)

private fun BufferedImage(
    width: Int,
    height: Int,
    hasAlpha: Boolean,
    withGraphics: (Graphics2D.() -> Unit)? = null,
): BufferedImage = BufferedImage(
    width,
    height,
    if (hasAlpha) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB,
).run { withGraphics?.let(::graphics) ?: this }

private fun <T> BufferedImage.graphics(block: Graphics2D.() -> T): BufferedImage = apply {
    val graphics = createGraphics()
    try {
        graphics.block()
    } finally {
        graphics.dispose()
    }
}

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
