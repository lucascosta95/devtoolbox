import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(libs.versions.jvmTarget.get().toInt())

    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":designsystem"))
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

val jvmMainRuntime: FileCollection = kotlin.targets.getByName("jvm")
    .compilations.getByName("main")
    .let { it.output.allOutputs + it.runtimeDependencyFiles!! }

tasks.register<JavaExec>("screenshot") {
    group = "verification"
    description = "Renderiza uma tela por ferramenta em build/screenshots"
    classpath = jvmMainRuntime
    mainClass.set("dev.devtoolbox.app.ScreenshotKt")
    dependsOn("jvmMainClasses")
}

tasks.register<JavaExec>("demoFrames") {
    group = "verification"
    description = "Renderiza os quadros do vídeo de demonstração em build/demo"
    classpath = jvmMainRuntime
    mainClass.set("dev.devtoolbox.app.DemoKt")
    dependsOn("jvmMainClasses")
}

tasks.register<JavaExec>("appIcon") {
    group = "build"
    description = "Gera src/jvmMain/resources/icons/devtoolbox.png"
    classpath = jvmMainRuntime
    mainClass.set("dev.devtoolbox.app.MakeIconKt")
    dependsOn("jvmMainClasses")
}

tasks.matching { it.name.startsWith("package") || it.name == "createDistributable" }
    .configureEach { dependsOn("appIcon") }

val appMainClass = "dev.devtoolbox.app.MainKt"

// AWT deriva o WM_CLASS da janela a partir da main class (pontos -> hífens). O .desktop
// gerado pelo jpackage no Linux não define StartupWMClass, então o GNOME Shell não
// associa a janela em execução ao .desktop e cai no ícone genérico (engrenagem do Yaru)
// na dock/alt-tab, mesmo com o ícone correto no menu de aplicativos.
val linuxStartupWMClass = appMainClass.replace('.', '-')

tasks.register("fixLinuxDesktopIcon") {
    group = "build"
    description = "Adiciona StartupWMClass ao .desktop empacotado no .deb para a dock do GNOME reconhecer o ícone"
    onlyIf { org.gradle.internal.os.OperatingSystem.current().isLinux }

    val debDir = layout.buildDirectory.dir("compose/binaries/main/deb")
    inputs.dir(debDir)
    outputs.dir(debDir)

    fun run(vararg command: String) {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor() == 0) { "${command.joinToString(" ")} falhou:\n$output" }
    }

    doLast {
        val debFiles = debDir.get().asFile.listFiles { f -> f.extension == "deb" }.orEmpty()
        for (deb in debFiles) {
            val tmp = temporaryDir.resolve(deb.nameWithoutExtension).apply { deleteRecursively(); mkdirs() }
            run("dpkg-deb", "-R", deb.absolutePath, tmp.absolutePath)

            val desktopFile = tmp.walkTopDown()
                .firstOrNull { it.isFile && it.extension == "desktop" && it.path.contains("${File.separator}lib${File.separator}") }
                ?: error("arquivo .desktop não encontrado em ${deb.name}")

            val text = desktopFile.readText()
            if ("StartupWMClass=" !in text) {
                val patched = text.lineSequence().joinToString("\n") { line ->
                    if (line.startsWith("Icon=")) "$line\nStartupWMClass=$linuxStartupWMClass" else line
                }
                desktopFile.writeText("$patched\n")
            }

            tmp.resolve("DEBIAN").listFiles { f -> f.name in setOf("preinst", "postinst", "prerm", "postrm") }
                ?.forEach { it.setExecutable(true) }

            deb.delete()
            run("dpkg-deb", "--root-owner-group", "-Zxz", "-b", tmp.absolutePath, deb.absolutePath)
            tmp.deleteRecursively()
        }
    }
}

tasks.matching { it.name == "packageDeb" }.configureEach { finalizedBy("fixLinuxDesktopIcon") }

compose.desktop.application {
    mainClass = appMainClass

    nativeDistributions {
        targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
        modules("java.instrument", "java.net.http", "jdk.unsupported")
        packageName = "DevToolbox"
        packageVersion = providers.gradleProperty("devtoolbox.version").get()
        description = "Caixa de ferramentas para desenvolvedores"
        vendor = "DevToolbox"

        fun iconOf(extension: String) =
            project.file("src/jvmMain/resources/icons/devtoolbox.$extension")

        macOS {
            bundleID = "dev.devtoolbox.app"
            iconFile.set(iconOf("icns"))
            dockName = "DevToolbox"
        }
        windows {
            menuGroup = "DevToolbox"
            shortcut = true
            menu = true
            iconFile.set(iconOf("ico"))
            upgradeUuid = "6f2b7c1e-9a3d-4c5f-8b0a-1d2e3f4a5b6c"
        }
        linux {
            packageName = "devtoolbox"
            menuGroup = "Development"
            iconFile.set(iconOf("png"))
        }
    }
}
