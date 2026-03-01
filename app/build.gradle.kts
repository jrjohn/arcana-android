plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktorfit)
    alias(libs.plugins.dokka)
    id("com.example.arcana.verification.ArchitectureVerificationPlugin")
}

android {
    namespace = "com.example.arcana"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.arcana"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            buildConfigField("String", "API_BASE_URL", "\"https://reqres.in/api/\"")
            buildConfigField("String", "API_KEY", "\"reqres-free-v1\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
        }
        release {
            isMinifyEnabled = false // NOSONAR kotlin:S7204
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_BASE_URL", "\"https://reqres.in/api/\"")
            buildConfigField("String", "API_KEY", "\"reqres-free-v1\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            excludes += "**/libdatastore_shared_counter.so"
            excludes += "**/libandroidx.graphics.path.so"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.timber)
    implementation(libs.androidx.work.runtime.ktx)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)


    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // ViewModel Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Ktorfit
    implementation(libs.ktorfit.lib)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Unit Test
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Configure test task
tasks.withType<Test> {
    // Suppress OpenJDK warning about bootstrap classpath
    jvmArgs("-XX:+IgnoreUnrecognizedVMOptions", "-Xshare:off")

    // Show test results
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
        showExceptions = true
        showCauses = true
        showStackTraces = true

        // Show summary after test execution
        afterSuite(KotlinClosure2<TestDescriptor, TestResult, Unit>({ desc, result ->
            if (desc.parent == null) { // Only execute for the whole test suite
                println("\n📊 Test Results:")
                println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                println("  Total:   ${result.testCount}")
                println("  ✅ Passed:  ${result.successfulTestCount}")
                println("  ❌ Failed:  ${result.failedTestCount}")
                println("  ⏭️  Skipped: ${result.skippedTestCount}")
                println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                println("  Result: ${result.resultType}")
                println("  Duration: ${result.endTime - result.startTime}ms")
                println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            }
        }))
    }

    // Continue running tests even if some fail (to see all failures)
    ignoreFailures = false

    // Run tests in parallel for faster execution
    maxParallelForks = Runtime.getRuntime().availableProcessors().div(2).coerceAtLeast(1)
}

// Make build task depend on test
tasks.named("build") {
    dependsOn("test")
}

// ============================================
// API Documentation Configuration (Dokka V2)
// ============================================

// Configure Dokka
dokka {
    moduleName.set("Arcana Android")

    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("docs/api"))

        // Suppress obvious functions
        suppressObviousFunctions.set(false)
    }

    dokkaSourceSets.configureEach {
        // Include all source sets
        suppressGeneratedFiles.set(false)

        // Skip test sources
        suppressedFiles.from(
            fileTree(projectDir.resolve("src/test")),
            fileTree(projectDir.resolve("src/androidTest"))
        )

        // Custom documentation sections (optional - uncomment if you have a Module.md file)
        // includes.from("Module.md")
    }
}

// Generate docs task
tasks.register("generateApiDocs") {
    group = "documentation"
    description = "Generates API documentation in HTML format"

    dependsOn("dokkaGeneratePublicationHtml")

    doLast {
        val docsDir = layout.buildDirectory.dir("docs/api").get().asFile
        println("")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("📚 API Documentation Generated Successfully!")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("  Location: ${docsDir.absolutePath}")
        println("  Format:   HTML")
        println("")
        println("  To view the documentation:")
        println("  open ${docsDir.resolve("index.html").absolutePath}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("")
    }
}

// Auto-generate documentation on every build
tasks.matching { it.name.matches(Regex("(assemble|compile.*Kotlin)")) }.configureEach {
    finalizedBy("generateApiDocs")
}

// Also generate on explicit build task
tasks.named("build") {
    finalizedBy("generateApiDocs")
}

// Also create a convenient task to build with docs
tasks.register("assembleWithDocs") {
    group = "build"
    description = "Assembles the app and generates API documentation"
    dependsOn("assemble", "generateApiDocs")
}

// ============================================
// Architecture Diagrams (Mermaid → Draw.io)
// ============================================

tasks.register<Exec>("generateMermaidDiagrams") {
    group = "documentation"
    description = "Generates architecture diagrams from Mermaid to PNG and Draw.io format"

    val docsDir = projectDir.resolve("../docs/architecture")
    val outputDir = layout.buildDirectory.dir("docs/diagrams").get().asFile

    doFirst {
        // Check if mmdc (mermaid-cli) is available
        val isMmdcAvailable = try {
            val process = ProcessBuilder("which", "mmdc").start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }

        if (!isMmdcAvailable) {
            println("")
            println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            println("⚠️  Mermaid CLI (mmdc) not found!")
            println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            println("")
            println("  To generate diagrams, install mermaid-cli:")
            println("  npm install -g @mermaid-js/mermaid-cli")
            println("")
            println("  Or use the online converter:")
            println("  1. Open: https://mermaid.live")
            println("  2. Paste content from: docs/architecture/*.mmd")
            println("  3. Export as PNG or SVG")
            println("")
            println("  Or import into draw.io:")
            println("  1. Open: https://app.diagrams.net")
            println("  2. File → Import from → Mermaid")
            println("  3. Select .mmd files from docs/architecture/")
            println("")
            println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            println("")

            // Still list available diagrams
            docsDir.listFiles()?.filter { it.extension == "mmd" }?.forEach { file ->
                println("  📄 ${file.name}")
            }
            println("")

            throw GradleException("Mermaid CLI not installed. See instructions above.")
        }

        // Create output directory
        outputDir.mkdirs()
    }

    // Generate PNG diagrams from all .mmd files
    val script = """
        cd ${docsDir.absolutePath} &&
        for f in *.mmd; do
            echo "Generating ${'$'}f..." &&
            mmdc -i "${'$'}f" -o "${outputDir.absolutePath}/${'$'}{f%.mmd}.png" -w 2048 -H 1536 -b transparent
        done
    """.trimIndent()

    commandLine("sh", "-c", script)

    doLast {
        println("")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("🎨 Architecture Diagrams Generated Successfully!")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("  PNG Location: ${outputDir.absolutePath}")
        println("  Mermaid Source: ${docsDir.absolutePath}")
        println("")
        println("  Diagrams generated:")

        outputDir.listFiles()?.filter { it.extension == "png" }?.forEach { file ->
            println("    ✓ ${file.name}")
        }

        println("")
        println("  To convert to Draw.io:")
        println("  1. Open https://app.diagrams.net")
        println("  2. File → Import from → Mermaid")
        println("  3. Select .mmd files from docs/architecture/")
        println("  4. File → Export as → XML (.drawio)")
        println("")
        println("  Or view online:")
        println("  Open https://mermaid.live and paste .mmd content")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("")
    }
}

// Task to list available diagrams without generating
tasks.register("listDiagrams") {
    group = "documentation"
    description = "Lists all available Mermaid architecture diagrams"

    doLast {
        val docsDir = projectDir.resolve("../docs/architecture")

        println("")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("📊 Available Architecture Diagrams")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("")

        docsDir.listFiles()?.filter { it.extension == "mmd" }?.sortedBy { it.name }?.forEach { file ->
            println("  ${file.name}")
            println("    ${file.absolutePath}")
            println("")
        }

        println("  To generate diagrams: ./gradlew generateMermaidDiagrams")
        println("  To view online: Copy content to https://mermaid.live")
        println("  To edit: Open .mmd files in VS Code with Mermaid extension")
        println("")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("")
    }
}

// Optional: Auto-generate diagrams with documentation
tasks.named("generateApiDocs") {
    doLast {
        println("  💡 Tip: Generate architecture diagrams with:")
        println("     ./gradlew generateMermaidDiagrams")
        println("")
    }
}

// ============================================
// Copy Documentation to Project Docs
// ============================================

tasks.register<Copy>("copyDocsToProject") {
    group = "documentation"
    description = "Copies generated API docs and diagrams to project/docs directory"

    dependsOn("generateApiDocs")

    // Copy API documentation
    from(layout.buildDirectory.dir("docs/api"))
    into(projectDir.resolve("../docs/api"))

    doLast {
        println("")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("📋 API Documentation copied to project docs!")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("  Destination: ${projectDir.resolve("../docs/api").absolutePath}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("")
    }
}

tasks.register<Copy>("copyDiagramsToProject") {
    group = "documentation"
    description = "Copies generated diagrams to project/docs/diagrams directory"

    dependsOn("generateMermaidDiagrams")

    // Copy diagram files
    from(layout.buildDirectory.dir("docs/diagrams"))
    into(projectDir.resolve("../docs/diagrams"))
    include("*.png")

    doLast {
        val targetDir = projectDir.resolve("../docs/diagrams")
        println("")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("🎨 Diagrams copied to project docs!")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("  Destination: ${targetDir.absolutePath}")
        println("")
        println("  Copied diagrams:")
        targetDir.listFiles()?.filter { it.extension == "png" }?.forEach { file ->
            println("    ✓ ${file.name}")
        }
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("")
    }
}

// Composite task to copy all documentation
tasks.register("copyAllDocsToProject") {
    group = "documentation"
    description = "Copies all generated documentation (API docs and diagrams) to project/docs"

    dependsOn("copyDocsToProject", "copyDiagramsToProject")
}

// Auto-copy documentation to project docs on build
tasks.named("generateApiDocs") {
    finalizedBy("copyDocsToProject")
}

tasks.named("generateMermaidDiagrams") {
    finalizedBy("copyDiagramsToProject")
}
