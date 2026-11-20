import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val webOnly = providers.gradleProperty("webOnly").orNull?.toBoolean() == true
val androidOnly = providers.gradleProperty("androidOnly").orNull?.toBoolean() == true

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.compose")
}

if (!webOnly) {
    apply(plugin = "com.android.library")
}

kotlin {
    if (!webOnly) {
        androidTarget {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
    } else {
        jvm {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
    }

    if (!androidOnly) {
        @OptIn(ExperimentalWasmDsl::class)
        wasmJs {
            browser()
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.animation)
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
        }

        if (!webOnly) {
            androidMain {
                kotlin.srcDir("src/jvmCommon/kotlin")
                dependencies {
                    implementation("androidx.activity:activity-compose:1.9.3")
                }
            }
        } else {
            jvmMain {
                kotlin.srcDir("src/jvmCommon/kotlin")
            }
        }

        if (!androidOnly) {
            wasmJsMain.dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-browser:0.3")
            }
        }
    }
}

if (!webOnly) {
    extensions.configure<LibraryExtension> {
        namespace = "com.bunkerstock.shared"
        compileSdk = 35
        defaultConfig {
            minSdk = 26
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}
