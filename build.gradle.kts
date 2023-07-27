import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("jvm") version "1.9.0"
    kotlin("kapt") version "1.9.0"
    id("org.beryx.runtime") version "1.13.0"
    application
}

group = "com.github.secretx33"
version = "0.1"

val javaVersion = 17

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.apache.pdfbox:pdfbox:3.0.0-beta1")
    implementation("com.1stleg:jnativehook:2.1.0")
    implementation("info.picocli:picocli:4.7.4")
    kapt("info.picocli:picocli-codegen:4.7.4")
    implementation("org.fusesource.jansi:jansi:2.4.0")
}

kapt {
    arguments {
        arg("project", "${project.group}/${project.name}")
    }
}

tasks.test { useJUnitPlatform() }

tasks.jar { enabled = false }

artifacts.archives(tasks.shadowJar)

tasks.shadowJar {
    archiveFileName.set("${rootProject.name}.jar")
}

tasks.withType<JavaCompile> {
    options.apply {
        release.set(javaVersion)
        options.encoding = "UTF-8"
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=all")
        jvmTarget = javaVersion.toString()
    }
}

val mainClassName = "com.github.secretx33.imagetopdf.ImageToPdfKt"

application {
    mainClass.set(mainClassName)
}

runtime {
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    modules.set(listOf("java.base", "java.desktop", "java.logging"))
    launcher {
        jvmArgs = listOf("-Xms1m", "-Xmx1024m", "-XX:+UseG1GC", "-XX:+DisableExplicitGC", "-Dfile.encoding=UTF-8")
    }
    jpackage {
        val currentOs = OperatingSystem.current()
        mainJar = shadowJar.archiveFileName.get()
        version = rootProject.version
        when {
            currentOs.isWindows -> {
                installerOptions = listOf(
                    "--description", rootProject.description,
                    "--win-menu",
                    "--win-menu-group", "ImageToPdf",
                    "--win-dir-chooser",
                )
                imageOptions = listOf("--win-console")
            }
            currentOs.isMacOsX -> {
                installerOptions = listOf("--mac-package-name", "ImageToPdf")
                installerType = "dmg"
            }
            else -> installerOptions = listOf("--linux-package-name", "ImageToPdf")
        }
    }
}

/**
 * Workaround for Warp [issue #56](https://github.com/dgiagio/warp/issues/56).
 */
val deleteWarpCachedBuild by tasks.registering(Delete::class) {
    group = "build"
    // Discover where this folder sits on Linux and MacOS, and remove them here too
    val cachedBuildFolder = when {
        currentOs.isWindows -> Path("C:/Users/${System.getProperty("user.name")}/AppData/Local/warp/packages/${rootProject.name}.exe")
        else -> null
    }
    cachedBuildFolder?.let { delete(it) }
}

/**
 * Generates the application binary at `build/app`.
 */
val createExecutable by tasks.registering(Exec::class) {
    group = "build"
    dependsOn(jpackageImage, deleteWarpCachedBuild)

    val (warpBinary, arch) = when {
        currentOs.isWindows -> "tools/warp-packer_windows-x64.exe" to "windows-x64"
        currentOs.isMacOsX -> "tools/warp-packer_macos-x64" to "macos-x64"
        else -> "tools/warp-packer_linux-x64" to "linux-x64"
    }
    val appBinaryName = if (currentOs.isWindows) "${rootProject.name}.exe" else rootProject.name

    doFirst {
        Path("$buildDir/app").createDirectories()
        println("""
            CurrentOS: ${currentOs.name}
            Arch: $arch
            Warp binary: $warpBinary
            App binary name: $appBinaryName
        """.trimIndent())
    }

    commandLine(
        warpBinary,
        "--arch", arch,
        "--input_dir", "./build/jpackage/${rootProject.name}",
        "--exec", appBinaryName,
        "--output", "./build/app/$appBinaryName",
    )
}

private val shadowJar: ShadowJar
    get() = tasks.getByName<ShadowJar>("shadowJar")

private val jpackageImage: Task
    get() = tasks.getByName<Task>("jpackageImage")

private val currentOs: OperatingSystem
    get() = OperatingSystem.current()