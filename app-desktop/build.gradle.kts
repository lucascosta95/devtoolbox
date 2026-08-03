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

/** Classpath do source set jvmMain, usado pelas tarefas utilitárias abaixo. */
val jvmMainRuntime: FileCollection = kotlin.targets.getByName("jvm")
    .compilations.getByName("main")
    .let { it.output.allOutputs + it.runtimeDependencyFiles!! }

/** Renderiza a UI para PNG sem display — conferência visual contra o protótipo. */
tasks.register<JavaExec>("screenshot") {
    group = "verification"
    description = "Renderiza uma tela por ferramenta em build/screenshots"
    classpath = jvmMainRuntime
    mainClass.set("dev.devtoolbox.app.ScreenshotKt")
    dependsOn("jvmMainClasses")
}

/** Gera o ícone do app a partir dos tokens do design system. */
tasks.register<JavaExec>("appIcon") {
    group = "build"
    description = "Gera src/jvmMain/resources/icons/devtoolbox.png"
    classpath = jvmMainRuntime
    mainClass.set("dev.devtoolbox.app.MakeIconKt")
    dependsOn("jvmMainClasses")
}

// Os ícones são derivados dos tokens do design system, não binários versionados: qualquer
// empacotamento os regenera antes, para nunca saírem de sincronia com o tema.
tasks.matching { it.name.startsWith("package") || it.name == "createDistributable" }
    .configureEach { dependsOn("appIcon") }

compose.desktop.application {
    mainClass = "dev.devtoolbox.app.MainKt"

    nativeDistributions {
        targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
        packageName = "DevToolbox"
        packageVersion = "1.0.0"
        description = "Caixa de ferramentas para desenvolvedores"
        vendor = "DevToolbox"

        // Um formato por plataforma; todos gerados pela tarefa `appIcon`.
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
            // UUID fixo: é o que permite ao instalador reconhecer e atualizar a versão anterior.
            upgradeUuid = "6f2b7c1e-9a3d-4c5f-8b0a-1d2e3f4a5b6c"
        }
        linux {
            packageName = "devtoolbox"
            menuGroup = "Development"
            iconFile.set(iconOf("png"))
        }
    }
}
