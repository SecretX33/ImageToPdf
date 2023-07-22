@file:OptIn(ExperimentalTime::class)

package com.github.secretx33.imagetopdf

import com.github.secretx33.imagetopdf.convert.addImage
import com.github.secretx33.imagetopdf.convert.createPdf
import com.github.secretx33.imagetopdf.exception.QuitApplicationException
import com.github.secretx33.imagetopdf.model.CombineMode
import com.github.secretx33.imagetopdf.model.Settings
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
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
    printGreetings()
    val settings = fetchSettings(args)
    val duration = measureTime {
        createPdfs(settings)
    }
    printSuccessMessage(settings, duration)
}

private fun printGreetings() {
    val banner = "$ANSI_RESET${getTextResource("banner.txt")}${System.lineSeparator().repeat(2)}"
    val monitoringStatus = "${ANSI_PURPLE}==> Please answer the following questions to generate the PDFs$ANSI_RESET${System.lineSeparator()}"
    println("$banner$monitoringStatus")
}

private fun createPdfs(settings: Settings) {
    println("Creating PDFs, sit back and relax, this can take a while...")
    if (settings.combineMode == CombineMode.SINGLE_FILE) {
        createSingleFile(settings)
    } else {
        createMultipleFiles(settings)
    }
}

private fun createSingleFile(settings: Settings) {
    val pdfFile = (settings.files.first().parent ?: Path("")) / "${settings.files.first().nameWithoutExtension}.pdf"
    createPdf(pdfFile) {
        settings.files.forEach { picture ->
            addImage(picture, settings)
        }
    }
    notifyPdfCreated(pdfFile)
}

private fun createMultipleFiles(settings: Settings) {
    settings.files.forEach { picture ->
        val pdfFile = (picture.parent ?: Path("")) / "${picture.nameWithoutExtension}.pdf"
        createPdf(pdfFile) {
            addImage(picture, settings)
        }
        notifyPdfCreated(pdfFile)
    }
}

private fun notifyPdfCreated(file: Path) =
    println("Created PDF $ANSI_GREEN'${file.name}'$ANSI_RESET $ANSI_PURPLE(${file.formattedFileSize()})$ANSI_RESET at $ANSI_GREEN'${file.parent.absolutePathString()}'$ANSI_RESET")

private fun printSuccessMessage(settings: Settings, duration: Duration) {
    val message = when {
        settings.files.size >= 2 && settings.combineMode == CombineMode.SINGLE_FILE -> "Successfully created a combined PDF from ${settings.files.size} images"
        settings.files.size >= 2 && settings.combineMode == CombineMode.MULTIPLE_FILES -> "Successfully created a PDF for each of the ${settings.files.size} images"
        else -> "Successfully created the PDF"
    }
    println("$message in ${duration.inWholeMilliseconds}ms.")
}
