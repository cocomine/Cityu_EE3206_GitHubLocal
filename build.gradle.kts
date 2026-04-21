import org.gradle.api.GradleException
import org.gradle.api.file.DuplicatesStrategy
import java.io.File

plugins {
    java
    application
    id("com.gradleup.shadow") version "9.0.0"
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "edu.year3.gitgui"
version = "1.0.0"

val osName = System.getProperty("os.name").lowercase()
val osArch = System.getProperty("os.arch").lowercase()
val detectedJavafxPlatform = when {
    osName.contains("mac") && (osArch == "aarch64" || osArch == "arm64") -> "mac-aarch64"
    osName.contains("mac") -> "mac"
    osName.contains("win") -> "win"
    osName.contains("linux") -> "linux"
    else -> throw GradleException("Unsupported OS/arch for JavaFX fat JAR: $osName / $osArch")
}
val supportedJavafxPlatforms = setOf("mac-aarch64", "mac", "win", "linux")
val targetJavafxPlatform = ((findProperty("targetPlatform") as String?) ?: detectedJavafxPlatform)
    .trim()
    .lowercase()

if (targetJavafxPlatform !in supportedJavafxPlatforms) {
    throw GradleException(
        "Unsupported -PtargetPlatform=$targetJavafxPlatform. " +
            "Supported values: ${supportedJavafxPlatforms.sorted().joinToString(", ")}"
    )
}

val appPackageName = "gitgui-local"
val appVendor = "EE3206 Team"
val isWindows = osName.contains("win")
val isMac = osName.contains("mac")
val isLinux = osName.contains("linux")
val jpackageExecutable = File(
    File(System.getProperty("java.home"), "bin"),
    if (isWindows) "jpackage.exe" else "jpackage"
).absolutePath

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

javafx {
    version = "21.0.4"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("org.openjfx:javafx-base:${javafx.version}:$targetJavafxPlatform")
    implementation("org.openjfx:javafx-graphics:${javafx.version}:$targetJavafxPlatform")
    implementation("org.openjfx:javafx-controls:${javafx.version}:$targetJavafxPlatform")
    implementation("org.openjfx:javafx-fxml:${javafx.version}:$targetJavafxPlatform")
}

application {
    mainClass.set("ui.Launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    group = "build"
    description = "Build a single runnable JAR with all runtime dependencies."
    archiveClassifier.set("${targetJavafxPlatform}-all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "ui.Launcher"
    }
}

tasks.register("singleJar") {
    group = "build"
    description = "Alias for building the single runnable JAR."
    dependsOn("shadowJar")
}

tasks.register<Exec>("packageAppImage") {
    group = "distribution"
    description = "Build app image with bundled Java runtime for current OS."
    dependsOn("installDist")

    doFirst {
        val inputDir = layout.buildDirectory.dir("install/${project.name}/lib").get().asFile.absolutePath
        val outputDir = layout.buildDirectory.dir("jpackage").get().asFile.absolutePath
        val mainJar = "${project.name}-${project.version}.jar"
        val appImageDir = layout.buildDirectory.dir("jpackage/$appPackageName").get().asFile
        val macAppImageDir = layout.buildDirectory.dir("jpackage/$appPackageName.app").get().asFile

        if (appImageDir.exists()) appImageDir.deleteRecursively()
        if (macAppImageDir.exists()) macAppImageDir.deleteRecursively()

        commandLine(
            jpackageExecutable,
            "--type", "app-image",
            "--name", appPackageName,
            "--app-version", project.version.toString(),
            "--vendor", appVendor,
            "--input", inputDir,
            "--module-path", inputDir,
            "--main-jar", mainJar,
            "--main-class", "ui.Launcher",
            "--dest", outputDir,
            "--add-modules", "javafx.controls,javafx.fxml"
        )
    }
}

tasks.register<Exec>("packageInstaller") {
    group = "distribution"
    description = "Build OS installer with bundled Java runtime (dmg/exe/deb)."
    dependsOn("packageAppImage")
    onlyIf { isMac || isWindows || isLinux }

    doFirst {
        val outputDir = layout.buildDirectory.dir("jpackage").get().asFile.absolutePath
        val installerType = when {
            isMac -> "dmg"
            isWindows -> "exe"
            else -> "deb"
        }
        val appImagePath = if (isMac) "$outputDir/$appPackageName.app" else "$outputDir/$appPackageName"
        val outputRoot = layout.buildDirectory.dir("jpackage").get().asFile

        outputRoot.listFiles()
            ?.filter { it.isFile && it.name.startsWith(appPackageName) && it.extension == installerType }
            ?.forEach { it.delete() }

        val installerArgs = mutableListOf(
            jpackageExecutable,
            "--type", installerType,
            "--name", appPackageName,
            "--app-version", project.version.toString(),
            "--vendor", appVendor,
            "--app-image", appImagePath,
            "--dest", outputDir
        )

        if (isWindows) {
            installerArgs.add("--win-shortcut")
        }

        commandLine(installerArgs)
    }
}
