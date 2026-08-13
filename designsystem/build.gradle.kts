@file:OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(libs.versions.jvmTarget.get().toInt())

    jvm()

    sourceSets {
        commonMain.dependencies {
            api(project(":core-tools"))
            api(compose.runtime)
            api(compose.foundation)
            api(compose.ui)
            api(compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(compose.uiTest)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "dev.devtoolbox.ds.resources"
}
