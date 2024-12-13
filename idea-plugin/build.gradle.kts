plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij.platform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3")
        instrumentationTools()
        bundledPlugin("com.intellij.gradle")
        bundledPlugin("org.jetbrains.kotlin")
    }

    implementation(libs.firework.orchestration)
    implementation(libs.jewel.ide)
    implementation(compose.desktop.currentOs) {
        exclude(group = "org.jetbrains.compose.material")
        exclude(group = "org.jetbrains.kotlinx")
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "org.jetbrains.compose.hotreload"
        name = "Compose Hot Reload"
        version = "0.0.1"
        description = "Compose Hot Reload"
    }
}

tasks.named<JavaExec>("runIde") {
 jvmArgumentProviders += CommandLineArgumentProvider {
     listOf("-Didea.kotlin.plugin.use.k2=true")
 }
}
