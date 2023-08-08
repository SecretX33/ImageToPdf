import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("jvm") version "1.9.0"
    kotlin("kapt") version "1.9.0"
    id("org.graalvm.buildtools.native") version "0.9.23"
    application
}

group = "com.github.secretx33"
version = "0.1.2"

val javaVersion = 17

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.7.3"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.apache.pdfbox:pdfbox:3.0.0-beta1")
    implementation("com.1stleg:jnativehook:2.1.0")
    implementation("info.picocli:picocli:4.7.4")
    kapt("info.picocli:picocli-codegen:4.7.4")
    implementation("org.fusesource.jansi:jansi:2.4.0")
    implementation(platform("io.arrow-kt:arrow-stack:1.2.0"))
    implementation("io.arrow-kt:arrow-fx-coroutines")
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

tasks.named<BuildNativeImageTask>("nativeCompile") {
    val shadowJar = tasks.shadowJar.get()
    dependsOn(shadowJar)
    classpathJar.set(shadowJar.archiveFile)
    doLast {
        copy {
            val file = file("$buildDir/native/nativeCompile/${executableName.get()}").absolutePath
            println("File = $file")
            from(file).into("$buildDir/libs")
        }
    }
}

// https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html
graalvmNative {
    binaries {
        named("main") {
            resources.autodetect()
            resources.includedPatterns.add(""".*\.txt$""")
            buildArgs.addAll(
                "-R:MinHeapSize=1m",  // Xms
                "-R:MaxHeapSize=1g",  // Xmx
                "-H:Log=registerResource:2",  // Logs added resources
                "-march=native",
            )
            useFatJar.set(true)
        }
    }
    toolchainDetection = false
}