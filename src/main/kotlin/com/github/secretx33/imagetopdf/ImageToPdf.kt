package com.github.secretx33.imagetopdf

import com.github.secretx33.imagetopdf.convert.addImage
import com.github.secretx33.imagetopdf.convert.createPdf
import com.github.secretx33.imagetopdf.exception.QuitApplicationException
import com.github.secretx33.imagetopdf.model.CombineMode
import com.github.secretx33.imagetopdf.model.Settings
import com.github.secretx33.imagetopdf.util.ANSI_CYAN
import com.github.secretx33.imagetopdf.util.ANSI_GREEN
import com.github.secretx33.imagetopdf.util.ANSI_PURPLE
import com.github.secretx33.imagetopdf.util.ANSI_RESET
import com.github.secretx33.imagetopdf.util.formattedFileSize
import com.github.secretx33.imagetopdf.util.formattedSeconds
import com.github.secretx33.imagetopdf.util.printError
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.time.Duration
import kotlin.time.measureTime

fun main(args: Array<String>) {
    try {
        bootstrapApplication(args)
    } catch (t: Throwable) {
        when (t) {
            is QuitApplicationException -> t.message?.let(::printError)
            else -> printError("Error: ${t::class.simpleName}: ${t.message}\n${t.stackTraceToString()}")
        }
    }
}

private fun bootstrapApplication(args: Array<String>) {
    val settings = fetchSettings(args)
    val duration = measureTime {
        createPdfs(settings)
    }
    printSuccessMessage(settings, duration)
}

private fun createPdfs(settings: Settings) {
    println("${ANSI_PURPLE}Generating PDFs, sit back and relax, this can take a while...$ANSI_RESET")
    if (settings.combineMode == CombineMode.SINGLE_FILE) {
        createSingleFile(settings)
    } else {
        createMultipleFiles(settings)
    }
}

private fun createSingleFile(settings: Settings) {
    val pdfFile = createPdfPath(settings.files.first())
    createPdf(pdfFile) {
        settings.files.forEach { picture ->
            addImage(picture, settings)
        }
    }
    notifyPdfCreated(pdfFile)
}

private fun createMultipleFiles(settings: Settings) {
    settings.files.forEach { picture ->
        val pdfFile = createPdfPath(picture)
        createPdf(pdfFile) {
            addImage(picture, settings)
        }
        notifyPdfCreated(pdfFile)
    }
}

private fun createPdfPath(picture: Path): Path = (picture.parent ?: Path("")) / "${picture.nameWithoutExtension}.pdf"

private fun notifyPdfCreated(file: Path) =
    println("${ANSI_PURPLE}Created PDF $ANSI_GREEN'${file.name}'$ANSI_PURPLE at $ANSI_GREEN'${file.parent.absolutePathString()}' $ANSI_CYAN(${file.formattedFileSize()})$ANSI_RESET")

private fun printSuccessMessage(settings: Settings, duration: Duration) {
    val message = when {
        settings.files.size >= 2 && settings.combineMode == CombineMode.SINGLE_FILE -> "Successfully created the PDF from $ANSI_GREEN${settings.files.size}$ANSI_PURPLE images"
        settings.files.size >= 2 && settings.combineMode == CombineMode.MULTIPLE_FILES -> "Successfully created a PDF for each of the $ANSI_GREEN${settings.files.size}$ANSI_PURPLE images"
        else -> "Successfully created the PDF"
    }
    println("$ANSI_PURPLE$message in $ANSI_CYAN${duration.formattedSeconds()}s$ANSI_RESET")
}
