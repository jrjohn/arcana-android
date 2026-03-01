package com.example.arcana.verification

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

/**
 * Gradle task that verifies the project architecture compliance
 * Based on the architectural standards defined in ARCHITECTURE.md and VIEWMODEL_PATTERN.md
 */
open class ArchitectureVerificationTask : DefaultTask() {

    init {
        group = "verification"
        description = "Verifies architecture compliance: Clean Architecture, ViewModel patterns, and coding standards"
    }

    @TaskAction
    fun verify() { // NOSONAR kotlin:S3776
        // Determine project root (could be run from root or app directory)
        val projectRoot = if (project.name == "app") {
            project.rootProject.projectDir
        } else {
            project.projectDir
        }

        logger.lifecycle("")

        // Use the new YAML-based verification system
        val verification = ArchitectureVerification(projectRoot)
        val warnings = verification.verify()

        logger.lifecycle("")

        // Print summary
        if (warnings.isEmpty()) {
            logger.lifecycle("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            logger.lifecycle("✅ Architecture Verification PASSED")
            logger.lifecycle("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            logger.lifecycle("")
            logger.lifecycle("   No violations found! 🎉")
        } else {
            val errorCount = warnings.count { it.severity == ArchitectureVerification.Severity.ERROR }
            val warningCount = warnings.count { it.severity == ArchitectureVerification.Severity.WARNING }
            val infoCount = warnings.count { it.severity == ArchitectureVerification.Severity.INFO }

            logger.lifecycle("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            logger.lifecycle("📊 Verification Summary")
            logger.lifecycle("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            logger.lifecycle("")
            logger.lifecycle("   ❌ Errors:   $errorCount")
            logger.lifecycle("   ⚠️  Warnings: $warningCount")
            logger.lifecycle("   ℹ️  Info:     $infoCount")
            logger.lifecycle("   📝 Total:    ${warnings.size}")
            logger.lifecycle("")

            // Group by category and print
            val byCategory = warnings.groupBy { it.category }

            byCategory.forEach { (category, violations) ->
                logger.lifecycle("   ${category.name.replace("_", " ")}:")
                violations.take(5).forEach { warning ->
                    val icon = when (warning.severity) {
                        ArchitectureVerification.Severity.ERROR -> "❌"
                        ArchitectureVerification.Severity.WARNING -> "⚠️"
                        ArchitectureVerification.Severity.INFO -> "ℹ️"
                    }
                    logger.lifecycle("      $icon ${warning.file}:${warning.line}")
                    if (warning.ruleId != null) {
                        logger.lifecycle("         ${warning.ruleId}: ${warning.message}")
                    } else {
                        logger.lifecycle("         ${warning.message}")
                    }
                }
                if (violations.size > 5) {
                    logger.lifecycle("      ... and ${violations.size - 5} more")
                }
                logger.lifecycle("")
            }

            // Check if we should fail the build
            if (errorCount > 0) {
                logger.lifecycle("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                logger.lifecycle("❌ Architecture Verification FAILED")
                logger.lifecycle("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                logger.lifecycle("")
                throw GradleException("Architecture verification failed with $errorCount errors")
            } else {
                logger.lifecycle("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                logger.lifecycle("✅ Architecture Verification PASSED (with warnings)")
                logger.lifecycle("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                logger.lifecycle("")
            }
        }
    }
}

// Legacy code removed - now using YAML-based verification system
// Old verification methods are kept in ArchitectureVerification.kt as fallback

/*
// Legacy verification methods (no longer used directly)

    private fun verifyProjectStructure(): VerificationResult {
        logger.lifecycle("📁 Checking project structure...")

        val baseDir = if (project.projectDir.name == "app") {
            "src/main/java/com/example/arcana"
        } else {
            "app/src/main/java/com/example/arcana"
        }

        val requiredDirs = listOf(
            "$baseDir/ui",
            "$baseDir/domain",
            "$baseDir/data",
            "$baseDir/core",
            "$baseDir/di"
        )

        val missingDirs = requiredDirs.filter { !project.file(it).exists() }

        return if (missingDirs.isEmpty()) {
            VerificationResult("Project Structure", true, "All required directories present")
        } else {
            VerificationResult("Project Structure", false, "Missing directories: $missingDirs")
        }
    }

    private fun verifyViewModelPattern(): VerificationResult {
        logger.lifecycle("🏗️  Checking ViewModel Input/Output pattern...")

        val baseDir = if (project.projectDir.name == "app") {
            "src/main/java/com/example/arcana/ui"
        } else {
            "app/src/main/java/com/example/arcana/ui"
        }

        val viewModelFiles = findKotlinFiles(baseDir)
            .filter { it.name.endsWith("ViewModel.kt") }

        if (viewModelFiles.isEmpty()) {
            return VerificationResult("ViewModel Pattern", true, "No ViewModels to check")
        }

        val violations = mutableListOf<String>()

        viewModelFiles.forEach { file ->
            val content = file.readText()
            val fileName = file.name

            // Check for required patterns
            if (!content.contains("sealed interface Input")) {
                violations.add("$fileName: Missing 'sealed interface Input'")
            }
            if (!content.contains("sealed interface Output")) {
                violations.add("$fileName: Missing 'sealed interface Output'")
            }
            if (!content.contains("data class State")) {
                violations.add("$fileName: Missing 'data class State' in Output")
            }
            if (!content.contains("sealed interface Effect")) {
                violations.add("$fileName: Missing 'sealed interface Effect' in Output")
            }
            if (!content.contains("fun onEvent(input: Input)")) {
                violations.add("$fileName: Missing 'fun onEvent(input: Input)'")
            }
            if (!content.contains("StateFlow<Output.State>") && !content.contains("StateFlow<${fileName.replace("ViewModel.kt", "")}ViewModel.Output.State>")) {
                violations.add("$fileName: Missing 'StateFlow<Output.State>'")
            }
            if (!content.contains("@HiltViewModel")) {
                violations.add("$fileName: Missing '@HiltViewModel' annotation")
            }
        }

        return if (violations.isEmpty()) {
            VerificationResult("ViewModel Pattern", true, "All ${viewModelFiles.size} ViewModels follow Input/Output pattern")
        } else {
            VerificationResult("ViewModel Pattern", false, violations.joinToString("\n"))
        }
    }

    private fun verifyDependencyInjection(): VerificationResult {
        logger.lifecycle("💉 Checking Dependency Injection...")

        val baseDir = if (project.projectDir.name == "app") {
            "src/main/java/com/example/arcana"
        } else {
            "app/src/main/java/com/example/arcana"
        }

        val violations = mutableListOf<String>()

        // Check ViewModels use @HiltViewModel
        val viewModelFiles = findKotlinFiles("$baseDir/ui")
            .filter { it.name.endsWith("ViewModel.kt") }

        viewModelFiles.forEach { file ->
            val content = file.readText()
            if (!content.contains("@HiltViewModel")) {
                violations.add("${file.name}: Missing @HiltViewModel annotation")
            }
            if (!content.contains("@Inject constructor")) {
                violations.add("${file.name}: Not using constructor injection")
            }
        }

        // Check for service locator anti-patterns
        val allKotlinFiles = findKotlinFiles(baseDir)
        allKotlinFiles.forEach { file ->
            val content = file.readText()
            if (content.contains(Regex("ServiceLocator|getInstance\\(\\)|INSTANCE"))) {
                violations.add("${file.name}: Possible service locator pattern detected")
            }
        }

        return if (violations.isEmpty()) {
            VerificationResult("Dependency Injection", true, "All classes use proper DI")
        } else {
            VerificationResult("Dependency Injection", false, violations.joinToString("\n"))
        }
    }

    private fun verifyDomainLayer(): VerificationResult {
        logger.lifecycle("🎯 Checking Domain Layer...")

        val baseDir = if (project.projectDir.name == "app") {
            "src/main/java/com/example/arcana/domain"
        } else {
            "app/src/main/java/com/example/arcana/domain"
        }

        val domainFiles = findKotlinFiles(baseDir)

        if (domainFiles.isEmpty()) {
            return VerificationResult("Domain Layer", true, "No domain files to check")
        }

        val violations = mutableListOf<String>()

        domainFiles.forEach { file ->
            val content = file.readText()

            // Check for Android framework dependencies (should have none)
            val androidImports = listOf(
                "import android.",
                "import androidx.",
                "import kotlinx.android"
            )

            androidImports.forEach { import ->
                if (content.contains(import)) {
                    violations.add("${file.name}: Contains Android dependency: $import")
                }
            }
        }

        return if (violations.isEmpty()) {
            VerificationResult("Domain Layer", true, "Domain layer has zero Android dependencies ✨")
        } else {
            VerificationResult("Domain Layer", false, violations.joinToString("\n"))
        }
    }

    private fun verifyDataLayer(): VerificationResult {
        logger.lifecycle("💾 Checking Data Layer...")

        val baseDir = if (project.projectDir.name == "app") {
            "src/main/java/com/example/arcana/data/repository"
        } else {
            "app/src/main/java/com/example/arcana/data/repository"
        }

        val repositoryFiles = findKotlinFiles(baseDir)
            .filter { it.name.endsWith("Repository.kt") && !it.name.endsWith("DataRepository.kt") }

        if (repositoryFiles.isEmpty()) {
            return VerificationResult("Data Layer", true, "No repository implementations to check")
        }

        val violations = mutableListOf<String>()

        repositoryFiles.forEach { file ->
            val content = file.readText()

            // Check for Flow usage
            if (!content.contains("import kotlinx.coroutines.flow.Flow")) {
                // Only flag if file has functions returning lists/collections
                if (content.contains(Regex("fun\\s+\\w+\\(.*\\):\\s*List<"))) {
                    violations.add("${file.name}: Should use Flow instead of direct List returns")
                }
            }

            // Check for proper error handling
            if (!content.contains("try") && !content.contains("Result<")) {
                violations.add("${file.name}: Missing error handling (try-catch or Result)")
            }
        }

        return if (violations.isEmpty()) {
            VerificationResult("Data Layer", true, "All repositories follow reactive patterns")
        } else {
            VerificationResult("Data Layer", false, violations.joinToString("\n"))
        }
    }

    private fun verifyTestCoverage(): VerificationResult {
        logger.lifecycle("🧪 Checking test structure...")

        val srcBaseDir = if (project.projectDir.name == "app") {
            "src/main/java/com/example/arcana"
        } else {
            "app/src/main/java/com/example/arcana"
        }

        val testBaseDir = if (project.projectDir.name == "app") {
            "src/test/java/com/example/arcana"
        } else {
            "app/src/test/java/com/example/arcana"
        }

        val srcFiles = findKotlinFiles(srcBaseDir)
            .filter {
                it.name.endsWith("ViewModel.kt") ||
                it.name.endsWith("Service.kt") ||
                it.name.endsWith("ServiceImpl.kt") ||
                it.name.endsWith("Repository.kt")
            }

        val testFiles = findKotlinFiles(testBaseDir)
            .filter { it.name.endsWith("Test.kt") }

        val violations = mutableListOf<String>()

        // Check that important classes have tests
        srcFiles.forEach { srcFile ->
            val expectedTestName = srcFile.name.replace(".kt", "Test.kt")
            val hasTest = testFiles.any { it.name == expectedTestName }

            if (!hasTest && !srcFile.name.contains("Interface")) {
                violations.add("Missing test for ${srcFile.name}")
            }
        }

        return if (violations.isEmpty()) {
            VerificationResult("Test Coverage", true, "All major components have tests")
        } else {
            // Don't fail on missing tests, just warn
            logger.warn("⚠️  Test coverage warnings:")
            violations.forEach { logger.warn("  - $it") }
            VerificationResult("Test Coverage", true, "Test structure verified (${violations.size} warnings)")
        }
    }

    private fun findKotlinFiles(path: String): List<File> {
        val dir = project.file(path)
        if (!dir.exists()) return emptyList()

        return dir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
    }

    private fun printResults(results: List<VerificationResult>) {
        logger.lifecycle("")
        logger.lifecycle("Results:")
        logger.lifecycle("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        results.forEach { result ->
            val icon = if (result.passed) "✅" else "❌"
            logger.lifecycle("$icon ${result.name}")
            logger.lifecycle("   ${result.message}")
            logger.lifecycle("")
        }
    }

    data class VerificationResult(
        val name: String,
        val passed: Boolean,
        val message: String
    )
}
*/
