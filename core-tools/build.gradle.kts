plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(libs.versions.jvmTarget.get().toInt())

    jvm()

    sourceSets {
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
