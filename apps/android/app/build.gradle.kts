plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "dev.counterline"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.counterline"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val ksFile = System.getenv("COUNTERLINE_KEYSTORE_FILE")
            if (ksFile != null) {
                storeFile = file(ksFile)
                storePassword = System.getenv("COUNTERLINE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("COUNTERLINE_KEY_ALIAS")
                keyPassword = System.getenv("COUNTERLINE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val ksFile = System.getenv("COUNTERLINE_KEYSTORE_FILE")
            signingConfig = if (ksFile != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            // Strip debug metadata from release builds
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // Do not strip debug symbols in debug builds for crash diagnostics
            keepDebugSymbols += "**//*.so"
        }
    }
}

// ── Build-time security verification ──────────────────────────────────────────

tasks.register("verifyReleaseBuildConfig") {
    group = "verification"
    description = "Fails if release build has insecure defaults"
    doLast {
        val appExtension = project.extensions.getByType<com.android.build.gradle.AppExtension>()
        val release = appExtension.buildTypes.getByName("release")

        check(release.isMinifyEnabled) {
            "SECURITY: Release build must have minification enabled (isMinifyEnabled = true)"
        }
        check(release.isShrinkResources) {
            "SECURITY: Release build must have resource shrinking enabled (isShrinkResources = true)"
        }
        check(!release.isDebuggable) {
            "SECURITY: Release build must not be debuggable"
        }
    }
}

tasks.register("verifyNoPlaintextSecrets") {
    group = "verification"
    description = "Scans for accidentally committed secrets"
    doLast {
        val patterns = listOf(
            Regex("""(?i)(password|secret|api[_-]?key|token)\s*=\s*["'][^"']+["']"""),
            Regex("""-----BEGIN (RSA |EC )?PRIVATE KEY-----"""),
            Regex("""AKIA[0-9A-Z]{16}"""), // AWS access key
        )
        val scanDirs = listOf(
            file("src"),
            file("../core"),
            file("../feature"),
        )
        var violations = 0
        scanDirs.filter { it.exists() }.forEach { dir ->
            dir.walkTopDown()
                .filter { it.isFile && it.extension in listOf("kt", "java", "xml", "json", "properties") }
                .filter { !it.path.contains("build/") }
                .forEach { file ->
                    file.readLines().forEachIndexed { index, line ->
                        patterns.forEach { pattern ->
                            if (pattern.containsMatchIn(line)) {
                                logger.error("SECURITY: Potential secret at ${file.relativeTo(rootDir)}:${index + 1}")
                                violations++
                            }
                        }
                    }
                }
        }
        check(violations == 0) {
            "SECURITY: Found $violations potential plaintext secret(s) in source files"
        }
    }
}

tasks.named("assembleRelease") {
    dependsOn("verifyReleaseBuildConfig")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":core:domain"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:engine"))
    implementation(project(":core:content"))

    implementation(project(":feature:home"))
    implementation(project(":feature:repertoire"))
    implementation(project(":feature:drill"))
    implementation(project(":feature:modelgames"))
    implementation(project(":feature:plans"))
    implementation(project(":feature:deviations"))
    implementation(project(":feature:exam"))
    implementation(project(":feature:progress"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:learn"))
    implementation(project(":feature:mistakereview"))
    implementation(project(":feature:quick5"))
    implementation(project(":feature:practice"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:blindfold"))
    implementation(project(":feature:coach"))
    implementation(project(":feature:notebook"))
    implementation(project(":feature:pgnimport"))
    implementation(project(":feature:preppack"))
    implementation(project(":feature:tacticalmotifs"))
    implementation(project(":feature:transitiontrainer"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.register<Exec>("extractContent") {
    description = "Regenerate content JSON assets from repo manifests"
    workingDir = rootProject.projectDir
    commandLine("python3", "scripts/extract_content.py")
}
