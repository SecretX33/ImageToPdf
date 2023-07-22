package com.github.secretx33.imagetopdf.model

import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.nio.file.Path
import kotlin.io.path.Path

@Command(name = "imagetopdf", version = ["ImageToPdf version 0.1"])
class CliParams {
    @Option(names = ["-c", "--combine"], description = ["Combine all files into a single PDF (default: false)"])
    var willCombine: Boolean = false

    @Option(names = ["-sf", "--scale-factor"], description = ["Scales the image resolution by this factor (default: 1.0)"])
    var imageScaleFactor: Double = 1.0

    @Option(names = ["-rf", "--render-factor"], description = ["Scales the image virtually in the final PDF by this factor (default: 0.5)"])
    var imageRenderFactor: Double = 0.5

    @Option(names = ["-i", "--interactive"], description = ["List all files and allowing reorder"])
    var isInteractive: Boolean = false

    @Parameters(paramLabel = "FILE", arity = "1..*", converter = [PathConverter::class], description = ["One or more files to add to PDF"])
    lateinit var files: Array<Path>

    @Option(names = ["-h", "--help"], usageHelp = true, description = ["Display this help message"])
    var usageHelpRequested = false

    @Option(names = ["-v", "--version"], versionHelp = true, description = ["Display version info"])
    var versionInfoRequested = false
}

fun CliParams.toSettings(): Settings {
    val fileSet = files.toSet()
    return Settings(
        files = fileSet,
        combineMode = if (fileSet.size >= 2 && !willCombine) CombineMode.MULTIPLE_FILES else CombineMode.SINGLE_FILE,
        imageScaleFactor = imageScaleFactor,
        imageRenderFactor = imageRenderFactor,
        isInteractive = isInteractive,
    )
}

class PathConverter : CommandLine.ITypeConverter<Path> {
    override fun convert(value: String): Path = Path(value)
}
