package com.github.secretx33.imagetopdf

import com.github.secretx33.imagetopdf.model.CliParams
import com.github.secretx33.imagetopdf.model.Settings
import com.github.secretx33.imagetopdf.model.SortFilesBy
import com.github.secretx33.imagetopdf.model.toSettings
import com.github.secretx33.imagetopdf.util.ANSI_GREEN
import com.github.secretx33.imagetopdf.util.ANSI_PURPLE
import com.github.secretx33.imagetopdf.util.ANSI_RED
import com.github.secretx33.imagetopdf.util.ANSI_RESET
import com.github.secretx33.imagetopdf.util.disableAnnoyingJnativehookLogger
import com.github.secretx33.imagetopdf.util.exitSilently
import com.github.secretx33.imagetopdf.util.exitWithMessage
import com.github.secretx33.imagetopdf.util.getTextResource
import org.jnativehook.GlobalScreen
import org.jnativehook.NativeHookException
import org.jnativehook.keyboard.NativeKeyEvent
import org.jnativehook.keyboard.NativeKeyListener
import picocli.CommandLine
import java.io.Closeable
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.Collections
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.notExists
import kotlin.io.path.readAttributes

fun fetchSettings(args: Array<String>): Settings {
    val cliParams = getCliParams(args).validate()
    printGreetings()
    return cliParams.toSettings()
        .sortFiles()
        .printSelectedOptions()
        .also { println() }
}

private fun getCliParams(args: Array<String>): CliParams {
    val cliParams = CliParams()
    val commandLine = CommandLine(cliParams).apply {
        isOptionsCaseInsensitive = true
        isCaseInsensitiveEnumValuesAllowed = true
    }
    commandLine.parseArgs(*args)
    if (cliParams.usageHelpRequested) {
        println("${getTextResource("banner_help.txt")}${System.lineSeparator()}")
        commandLine.usage(System.out)
        exitSilently()
    }
    if (cliParams.versionInfoRequested) {
        commandLine.printVersionHelp(System.out)
        exitSilently()
    }
    return cliParams
}

private fun CliParams.validate(): CliParams = apply {
    validatePaths(files)
    validateInRange(imageResizeFactor, "image resize factor", 0.0..1000000.0)
    validateInRange(imageRenderFactor, "image render factor", 0.0..1000000.0)
    validateInRange(jpgCompressionQuality, "compression value", 0.0..1.0)
}

private fun <T> validateInRange(
    number: T?,
    parameterName: String,
    range: ClosedRange<T>,
) where T : Number, T : Comparable<T> {
    if (number != null && number !in range) {
        exitWithMessage("Invalid argument: $parameterName '$number' is out of bounds (${range.start.toLong()} ~ ${range.endInclusive.toLong()})")
    }
}

private fun validatePaths(paths: Array<Path>) {
    if (paths.isEmpty()) {
        exitWithMessage("Invalid argument: this program requires at least one file passed as argument.")
    }
    paths.firstNotNullOfOrNull {
        when {
            it.notExists() -> "Invalid argument: file '$it' does not seems to exist."
            !it.isRegularFile() -> "Invalid argument: '$it' is not a file (maybe it is a folder?)."
            !it.isSupportedFormat() -> "Invalid argument: file '$it' is of an illegal type '${it.extension}', this program only support these extensions: ${supportedExtensions.sorted().joinToString(", ") { ".$it" }}."
            else -> null
        }
    }?.let(::exitWithMessage)
}

private val supportedExtensions = setOf("jpg", "jpeg", "png")

private fun Path.isSupportedFormat(): Boolean = supportedExtensions.any { extension.equals(it, ignoreCase = true) }

private fun printGreetings() = println("$ANSI_RESET${getTextResource("banner.txt")}${System.lineSeparator()}")

private fun Settings.sortFiles(): Settings = when {
    files.size >= 2 && isInteractive -> copy(files = interactivelyReorderFiles(files))
    files.size >= 2 && sortFilesBy != null -> copy(files = files.sortBy(sortFilesBy).toSet())
    else -> this
}

private fun Iterable<Path>.sortBy(sortFilesBy: SortFilesBy): List<Path> = when (sortFilesBy) {
    SortFilesBy.NAME -> sortedWith { o1, o2 ->
        String.CASE_INSENSITIVE_ORDER.compare(o1.absolutePathString(), o2.absolutePathString())
    }
    SortFilesBy.NAME_DESC -> sortedWith { o1, o2 ->
        String.CASE_INSENSITIVE_ORDER.compare(o2.absolutePathString(), o1.absolutePathString())
    }
    SortFilesBy.CREATED_DATE -> sortedBy { it.attributes.creationTime().toInstant() }
    SortFilesBy.CREATED_DATE_DESC -> sortedByDescending { it.attributes.creationTime().toInstant() }
    SortFilesBy.MODIFIED_DATE -> sortedBy { it.attributes.lastModifiedTime().toInstant() }
    SortFilesBy.MODIFIED_DATE_DESC -> sortedByDescending { it.attributes.lastModifiedTime().toInstant() }
}

private val Path.attributes: BasicFileAttributes
    get() = readAttributes<BasicFileAttributes>()

private fun interactivelyReorderFiles(files: Set<Path>): Set<Path> {
    val mutableFiles = files.toMutableList()

    println("${ANSI_PURPLE}Hint: Use your keyboard ${ANSI_GREEN}UP$ANSI_PURPLE and ${ANSI_GREEN}DOWN$ANSI_PURPLE arrows to navigate the files, ${ANSI_GREEN}Space$ANSI_PURPLE to swap their places$ANSI_PURPLE, and ${ANSI_GREEN}Q$ANSI_PURPLE, ${ANSI_GREEN}Enter$ANSI_PURPLE, or ${ANSI_GREEN}ESC$ANSI_PURPLE to confirm$ANSI_RESET")

    var isReordering = true
    var cursorIndex = 0
    var itemSelectedIndex: Int? = null
    val keyListener = SimpleNativeKeyListener()

    while (isReordering) {
        printCurrentFilesForReorder(mutableFiles, cursorIndex, itemSelectedIndex)
        when (getReorderOption(keyListener)) {
            ReorderOption.UP -> cursorIndex = (cursorIndex - 1).coerceAtLeast(0)
            ReorderOption.DOWN -> cursorIndex = (cursorIndex + 1).coerceAtMost(mutableFiles.lastIndex)
            ReorderOption.SELECT -> itemSelectedIndex = when (itemSelectedIndex) {
                null -> cursorIndex  // Select current item
                cursorIndex -> null  // Unselect current item
                else -> {            // Swap items
                    Collections.swap(mutableFiles, cursorIndex, itemSelectedIndex)
                    null
                }
            }
            ReorderOption.QUIT -> isReordering = false
        }
    }

    keyListener.close()
    return mutableFiles.toSet()
}

private fun printCurrentFilesForReorder(
    files: Iterable<Path>,
    cursorIndex: Int,
    itemSelectedIndex: Int?,
) {
    val fileSelector = files.mapIndexed { index, path ->
        val isHoveringItem = cursorIndex == index
        val isItemSelected = itemSelectedIndex == index

        buildString {
            append(
                when {
                    isHoveringItem && isItemSelected -> " $ANSI_PURPLE-> $ANSI_RED* "
                    isHoveringItem -> "   $ANSI_PURPLE-> "
                    isItemSelected -> "    $ANSI_RED* "
                    else -> "      "
                }
            )
            append("$ANSI_GREEN${index + 1}.$ANSI_RESET ${path.absolutePathString()}")
        }
    }.joinToString(System.lineSeparator(), prefix = "${System.lineSeparator()}${ANSI_PURPLE}Files: $ANSI_RESET${System.lineSeparator()}")
    println(fileSelector)
}

private fun getReorderOption(keyListener: SimpleNativeKeyListener): ReorderOption {
    // Blocks the main thread
    val event = keyListener.readKey().get()
    return when (event.keyCode) {
        NativeKeyEvent.VC_UP -> ReorderOption.UP
        NativeKeyEvent.VC_DOWN -> ReorderOption.DOWN
        NativeKeyEvent.VC_SPACE -> ReorderOption.SELECT
        NativeKeyEvent.VC_ENTER, NativeKeyEvent.VC_ESCAPE, NativeKeyEvent.VC_Q -> ReorderOption.QUIT
        else -> exitWithMessage("Error: Key number '$event' is not mapped.")
    }
}

private fun Settings.printSelectedOptions(): Settings = apply {
    println("${ANSI_PURPLE}Mode:$ANSI_GREEN ${combineMode.displayName}$ANSI_RESET")
    sortFilesBy?.let {
        println("${ANSI_PURPLE}Sort: $ANSI_GREEN${it.displayName}$ANSI_RESET")
    }
    jpgCompressionQuality?.let {
        println("${ANSI_PURPLE}JPG compression enabled. Quality: $ANSI_GREEN$it$ANSI_RESET")
    }
    printCurrentFiles(files)
}

private fun printCurrentFiles(files: Iterable<Path>) {
    val fileSelector = files.mapIndexed { index, path ->
        "    $ANSI_GREEN${index + 1}.$ANSI_RESET ${path.absolutePathString()}"
    }.joinToString(System.lineSeparator(), prefix = "${ANSI_PURPLE}Files: $ANSI_RESET${System.lineSeparator()}")
    println(fileSelector)
}

private enum class ReorderOption {
    UP,
    DOWN,
    SELECT,
    QUIT,
}

private class SimpleNativeKeyListener : NativeKeyListener, Closeable {

    private val isRegistered = AtomicBoolean(false)
    private val callback = AtomicReference<CompletableFuture<NativeKeyEvent>>(null)

    override fun nativeKeyTyped(event: NativeKeyEvent) = handleKeyPress(event)

    override fun nativeKeyPressed(event: NativeKeyEvent) = handleKeyPress(event)

    override fun nativeKeyReleased(event: NativeKeyEvent) {
    }

    private fun handleKeyPress(event: NativeKeyEvent) {
//        println("Press key (code = ${event.keyCode}, raw = ${event.rawCode}, name = ${NativeKeyEvent.getKeyText(event.keyCode)})")
        if (event.keyCode !in INTERESTED_KEYS) return
        val completableFuture = callback.get() ?: return
        if (callback.compareAndSet(completableFuture, null)) {
            completableFuture.complete(event)
        }
    }

    fun readKey(): CompletableFuture<NativeKeyEvent> {
        registerKeyListener()
        val future = CompletableFuture<NativeKeyEvent>()
        if (!callback.compareAndSet(null, future)) {
            throw IllegalStateException("Could not register callback because there is another callback registered already")
        }
        return future
    }

    /**
     * Sets up the global key listener by registering the native hook and adding this instance as a native key listener.
     */
    private fun registerKeyListener() {
        if (!isRegistered.compareAndSet(false, true)) return
        disableAnnoyingJnativehookLogger()
        try {
            GlobalScreen.registerNativeHook()
        } catch (e: NativeHookException) {
            exitWithMessage("${e.message}\n${e.stackTraceToString()}")
        }
        GlobalScreen.addNativeKeyListener(this)
    }

    override fun close() {
        try {
            GlobalScreen.unregisterNativeHook()
        } finally {
            GlobalScreen.removeNativeKeyListener(this)
            isRegistered.set(false)
        }
    }

    private companion object {
        val INTERESTED_KEYS = setOf(NativeKeyEvent.VC_UP, NativeKeyEvent.VC_DOWN, NativeKeyEvent.VC_ENTER, NativeKeyEvent.VC_SPACE, NativeKeyEvent.VC_ESCAPE, NativeKeyEvent.VC_Q)
    }
}