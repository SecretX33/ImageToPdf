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
    var isCombine: Boolean = false

    @Option(names = ["--resize"], description = ["Resize the image resolution by this factor (default: 1.0)"])
    var imageResizeFactor: Double = 1.0

    @Option(names = ["-rf", "--render-factor"], description = ["Scales the image virtually in the final PDF by this factor (default: 0.5)"])
    var imageRenderFactor: Double = 0.5

    @Option(names = ["-i", "--interactive"], description = ["List all files and allow re-order"])
    var isInteractive: Boolean = false

    @Option(names = ["-jq", "--jpg-quality"], arity = "0..1", fallbackValue = "0.75", description = ["Converts the images into JPEG with the provided quality (0.0 ~ 1.0) (disabled by default) (if specified without parameter: \${FALLBACK-VALUE})"])
    var jpgCompressionQuality: Double? = null

    @Option(names = ["-s", "--sort"], arity = "0..1", fallbackValue = "NAME", description = ["Sort the given FILE by the specified mode (disabled by default) (if specified without parameter: \${FALLBACK-VALUE})"])
    var sortFilesBy: SortFilesBy? = null

    @Parameters(paramLabel = "FILE", converter = [PathConverter::class], description = ["One or more files to add to PDF"])
    var files: Array<Path> = emptyArray()

    @Option(names = ["-h", "--help"], usageHelp = true, description = ["Display this help message"])
    var usageHelpRequested = false

    @Option(names = ["-v", "--version"], versionHelp = true, description = ["Display version info"])
    var versionInfoRequested = false
}

enum class SortFilesBy(val displayName: String) {
    NAME("Name"),
    NAME_DESC("Name (desc)"),
    CREATED_DATE("Created date"),
    CREATED_DATE_DESC("Created date (desc)"),
    MODIFIED_DATE("Modified date"),
    MODIFIED_DATE_DESC("Modified date (desc)"),
}

class PathConverter : CommandLine.ITypeConverter<Path> {
    override fun convert(value: String): Path = Path(value)
}
