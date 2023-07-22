package com.github.secretx33.imagetopdf

import com.github.secretx33.imagetopdf.model.CliParams
import com.github.secretx33.imagetopdf.model.Settings
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
import java.util.Collections
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.notExists

fun fetchSettings(args: Array<String>): Settings {
    val cliParams = getCliParams(args)
    validatePaths(cliParams.files)
    printGreetings()
    return cliParams.toSettings()
        .applyInteractive()
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

private fun validatePaths(paths: Array<Path>) {
    var errorMessage: String? = null

    if (paths.isEmpty()) {
        errorMessage = "Invalid argument: this program requires at least one file passed as argument to function."
    }
    paths.onEach {
        errorMessage = errorMessage ?: when {
            it.notExists() -> "Invalid argument: file '$it' does not seems to exist."
            !it.isSupportedFormat() -> "Invalid argument: file '$it' is of an illegal type '${it.extension}', this program only support these extensions: ${supportedExtensions.joinToString(", ") { ".$it" }}."
            !it.isRegularFile() -> "Invalid argument: '$it' is not a file (maybe it is a folder?)"
            else -> null
        }
    }
    errorMessage?.let(::exitWithMessage)
}

private val supportedExtensions = setOf("jpg", "jpeg", "png")

private fun Path.isSupportedFormat(): Boolean = supportedExtensions.any { extension.equals(it, ignoreCase = true) }

private fun printGreetings() = println("$ANSI_RESET${getTextResource("banner.txt")}${System.lineSeparator()}")

private fun Settings.applyInteractive(): Settings =
    if (files.size >= 2 && isInteractive) copy(files = reorderFiles(files)) else this

private fun reorderFiles(files: Set<Path>): Set<Path> {
    val files = files.toMutableList()

    println("${ANSI_PURPLE}Hint: Use your keyboard ${ANSI_GREEN}UP$ANSI_PURPLE and ${ANSI_GREEN}DOWN$ANSI_PURPLE arrows to navigate the files, ${ANSI_GREEN}Space$ANSI_PURPLE to swap their places$ANSI_PURPLE, and ${ANSI_GREEN}Q$ANSI_PURPLE, ${ANSI_GREEN}Enter$ANSI_PURPLE, or ${ANSI_GREEN}ESC$ANSI_PURPLE to confirm$ANSI_RESET")

    var isReordering = true
    var cursorIndex = 0
    var itemSelectedIndex: Int? = null
    val keyListener = SimpleNativeKeyListener()

    while (isReordering) {
        printCurrentFilesForReorder(files, cursorIndex, itemSelectedIndex)
        when (getReorderOption(keyListener)) {
            ReorderOption.UP -> cursorIndex = (cursorIndex - 1).coerceAtLeast(0)
            ReorderOption.DOWN -> cursorIndex = (cursorIndex + 1).coerceAtMost(files.lastIndex)
            ReorderOption.SELECT -> itemSelectedIndex = when (itemSelectedIndex) {
                null -> cursorIndex  // Select current item
                cursorIndex -> null  // Unselect current item
                else -> {            // Swap items
                    Collections.swap(files, cursorIndex, itemSelectedIndex)
                    null
                }
            }
            ReorderOption.QUIT -> isReordering = false
        }
    }

    keyListener.close()
    return files.toSet()
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

    override fun close() = GlobalScreen.removeNativeKeyListener(this)

    private companion object {
        val INTERESTED_KEYS = setOf(NativeKeyEvent.VC_UP, NativeKeyEvent.VC_DOWN, NativeKeyEvent.VC_ENTER, NativeKeyEvent.VC_SPACE, NativeKeyEvent.VC_ESCAPE, NativeKeyEvent.VC_Q)
    }
}