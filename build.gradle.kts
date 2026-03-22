import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.gradleup.shadow") version "9.4.0"
    kotlin("jvm") version "2.3.10"
    kotlin("kapt") version "2.3.10"
    id("org.graalvm.buildtools.native") version "0.11.5"
    application
}

group = "com.github.secretx33"
version = "0.2.5"

val javaVersion = JvmTarget.JVM_25

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.10.2"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.apache.pdfbox:pdfbox:3.0.7")
    implementation("com.1stleg:jnativehook:2.1.0")
    implementation("info.picocli:picocli:4.7.7")
    kapt("info.picocli:picocli-codegen:4.7.7")
    implementation("org.fusesource.jansi:jansi:2.4.2")
    implementation(platform("io.arrow-kt:arrow-stack:2.2.2.1"))
    implementation("io.arrow-kt:arrow-fx-coroutines")
    implementation("com.drewnoakes:metadata-extractor:2.19.0")
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
        release = javaVersion.target.toInt()
        options.encoding = "UTF-8"
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=all")
        jvmTarget = javaVersion
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
            runtimeArgs.add("-Djava.awt.headless=true")
            useFatJar.set(true)
        }
    }
    toolchainDetection = false
}
