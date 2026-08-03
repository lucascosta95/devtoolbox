plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

/**
 * A versão da aplicação chega ao código por um arquivo gerado, não por constante escrita à mão:
 * `gradle.properties` é a fonte única, e os instaladores leem a mesma propriedade.
 */
val appVersion: String = providers.gradleProperty("devtoolbox.version").get()

val generateBuildInfo = tasks.register("generateBuildInfo") {
    val outputDir = layout.buildDirectory.dir("generated/buildInfo/kotlin")
    inputs.property("version", appVersion)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("dev/devtoolbox/core/BuildInfo.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |// Gerado por :core-tools:generateBuildInfo — não edite à mão.
            |package dev.devtoolbox.core
            |
            |/** Metadados do build, preenchidos a partir de `gradle.properties`. */
            |object BuildInfo {
            |    const val VERSION: String = "$appVersion"
            |
            |    /** Como a UI mostra: "v1.2.0". */
            |    val displayVersion: String get() = "v" + VERSION
            |}
            |
            """.trimMargin(),
        )
    }
}

kotlin {
    jvmToolchain(libs.versions.jvmTarget.get().toInt())

    jvm()

    sourceSets {
        commonMain { kotlin.srcDir(generateBuildInfo) }
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        // ZXing é JVM-only e entra **apenas** em teste: o encoder de produção continua
        // sem dependências, mas a matriz que ele gera é conferida contra uma implementação
        // de referência.
        jvmTest.dependencies {
            implementation(libs.zxing.core)
        }
    }
}
