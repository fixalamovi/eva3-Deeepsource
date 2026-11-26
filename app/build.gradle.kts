import java.util.Properties
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

// Archivo privado que acompaña la entrega; no se publica en GitHub.
val releaseProperties = Properties().apply {
    val source = rootProject.file("firma/keystore.properties")
    if (source.exists()) source.inputStream().use { load(it) }
}
fun signingValue(name: String): String? = providers.environmentVariable("DEEPSOURCE_" + name).orNull
    ?: releaseProperties.getProperty(name)
val releaseStore = signingValue("STORE_FILE")
val hasReleaseSigning = listOf("STORE_FILE", "STORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD")
    .all { !signingValue(it).isNullOrBlank() }

android {
    namespace = "com.example.deepsource"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.deepsource"
        minSdk = 26
        targetSdk = 37
        versionCode = 8
        versionName = "1.7"
        buildConfigField("String", "PROJECTS_API_URL",
            quoted(providers.gradleProperty("PROJECTS_API_URL").getOrElse("http://10.0.2.2:8081/")))
        buildConfigField("String", "RECORDS_API_URL",
            quoted(providers.gradleProperty("RECORDS_API_URL").getOrElse("http://10.0.2.2:8082/")))
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    flavorDimensions += "backend"
    productFlavors {
        create("local") {
            dimension = "backend"
            buildConfigField("boolean", "USE_SERVER", "false")
        }
        create("server") {
            dimension = "backend"
            isDefault = true
            buildConfigField("boolean", "USE_SERVER", "true")
        }
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = rootProject.file(requireNotNull(releaseStore))
                storePassword = signingValue("STORE_PASSWORD")
                keyAlias = signingValue("KEY_ALIAS")
                keyPassword = signingValue("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            isDebuggable = false
            isMinifyEnabled = false
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Informe de lógica: incluye también las clases que todavía no tienen pruebas.
// Conservamos dominio, repositorios, ViewModels y el coordinador AppState.
listOf("serverDebug", "localDebug").forEach { variant ->
    val taskVariant = variant.replaceFirstChar { it.uppercase() }
    tasks.register<JacocoReport>("logic${taskVariant}Coverage") {
        dependsOn("test${taskVariant}UnitTest")
        reports {
            html.required.set(true)
            xml.required.set(true)
            html.outputLocation.set(layout.buildDirectory.dir("reports/coverage/logic/$variant"))
            xml.outputLocation.set(layout.buildDirectory.file("reports/coverage/logic/$variant/jacoco.xml"))
        }
        sourceDirectories.setFrom(files("src/main/java"))
        classDirectories.setFrom(provider {
            listOf(
                "intermediates/built_in_kotlinc/$variant/compile${taskVariant}Kotlin/classes",
                "tmp/kotlin-classes/$variant",
                "intermediates/javac/$variant/compile${taskVariant}JavaWithJavac/classes"
            ).map { layout.buildDirectory.dir(it).get().asFile }.filter { it.exists() }.map { directory ->
                fileTree(directory) {
                    include("com/example/deepsource/domain/**", "com/example/deepsource/data/**",
                        "com/example/deepsource/ui/viewmodel/**", "com/example/deepsource/AppState*")
                }
            }
        })
        executionData.setFrom(fileTree(layout.buildDirectory) {
            include("**/test${taskVariant}UnitTest.exec", "**/${variant}UnitTest.exec")
        })
        doFirst {
            check(!classDirectories.isEmpty) { "No se encontraron clases para medir cobertura" }
            check(!executionData.isEmpty) { "No se encontró la ejecución JaCoCo de las pruebas" }
        }
    }
}
