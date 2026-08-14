plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

val appVersion: String = providers.gradleProperty("devtoolbox.version").get()
val appRepo: String = providers.gradleProperty("devtoolbox.repo").get()

val generateBuildInfo = tasks.register("generateBuildInfo") {
    val outputDir = layout.buildDirectory.dir("generated/buildInfo/kotlin")
    inputs.property("version", appVersion)
    inputs.property("repo", appRepo)
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
            |    /** Repositório `dono/nome` no GitHub, usado na checagem de atualização. */
            |    const val REPO: String = "$appRepo"
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
        jvmTest.dependencies {
            implementation(libs.zxing.core)
        }
    }
}
