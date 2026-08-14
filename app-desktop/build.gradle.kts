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

compose.desktop.application {
    mainClass = "dev.devtoolbox.app.MainKt"

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
