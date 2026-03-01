package com.example.arcana.verification

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Architecture Verification System
 *
 * Loads rules from YAML files in .architecture-verification/rules/
 * and executes them against the codebase.
 *
 * Features:
 * - YAML-based rule definitions
 * - Configurable severity levels
 * - Extensible rule system
 * - Detailed violation reports
 */
class ArchitectureVerification(private val projectDir: File) {

    data class Warning(
        val severity: Severity,
        val category: Category,
        val file: String,
        val line: Int,
        val message: String,
        val suggestion: String? = null,
        val ruleId: String? = null
    )

    enum class Severity { ERROR, WARNING, INFO }
    enum class Category {
        ARCHITECTURE,
        VIEWMODEL_PATTERN,
        REPOSITORY_PATTERN,
        SERVICE_LAYER,
        MODEL_LAYER,
        LAYER_VIOLATION,
        NAMING_CONVENTION,
        MISSING_TEST,
        TEST_STRUCTURE,
        CODE_STYLE,
        DOCUMENTATION
    }

    private val warnings = mutableListOf<Warning>()
    private val yamlLoader = YamlRuleLoader(projectDir)
    private val ruleEngine = RuleEngine(projectDir)

    /**
     * Main verification method - loads and executes YAML rules
     */
    fun verify(): List<Warning> {
        println("🔍 Architecture Verification System")
        println("=" .repeat(60))
        println()

        // Check if rule directory exists
        val rulesDir = File(projectDir, ".architecture-verification/rules")
        if (!rulesDir.exists()) {
            println("⚠️  Warning: .architecture-verification/rules/ directory not found")
            println("   Falling back to legacy hardcoded checks...")
            println()
            return verifyLegacy()
        }

        println("📂 Loading rules from: ${rulesDir.absolutePath}")
        println()

        // Load YAML rules
        val ruleFiles = yamlLoader.loadAllRules()

        if (ruleFiles.isEmpty()) {
            println("⚠️  No rule files found. Falling back to legacy checks...")
            println()
            return verifyLegacy()
        }

        val totalRules = ruleFiles.sumOf { it.rules.count { rule -> rule.enabled } }
        println()
        println("📋 Loaded ${ruleFiles.size} rule files with $totalRules enabled rules")
        println()

        // Load configuration (optional)
        val config = yamlLoader.loadConfig()
        if (config != null) {
            println("⚙️  Configuration loaded")
        }
        println()

        // Execute rules
        println("🔍 Executing rules...")
        println()

        val violations = ruleEngine.executeRules(ruleFiles)

        // Convert violations to warnings
        violations.forEach { violation ->
            val severity = when (violation.severity.uppercase()) {
                "ERROR" -> Severity.ERROR
                "WARNING" -> Severity.WARNING
                "INFO" -> Severity.INFO
                else -> Severity.WARNING
            }

            val category = mapCategory(violation.category)

            warnings.add(
                Warning(
                    severity = severity,
                    category = category,
                    file = violation.file,
                    line = violation.line,
                    message = violation.message,
                    suggestion = violation.suggestion,
                    ruleId = violation.ruleId
                )
            )
        }

        return warnings
    }

    /**
     * Map YAML category to enum
     */
    private fun mapCategory(yamlCategory: String): Category {
        return when (yamlCategory.lowercase()) {
            "architecture" -> Category.ARCHITECTURE
            "viewmodel_pattern" -> Category.VIEWMODEL_PATTERN
            "repository_pattern" -> Category.REPOSITORY_PATTERN
            "service_layer" -> Category.SERVICE_LAYER
            "model_layer" -> Category.MODEL_LAYER
            "testing" -> Category.MISSING_TEST
            "test_structure" -> Category.TEST_STRUCTURE
            "coding_style" -> Category.CODE_STYLE
            "documentation" -> Category.DOCUMENTATION
            else -> Category.ARCHITECTURE
        }
    }

    /**
     * Legacy verification (fallback when YAML rules not available)
     */
    private fun verifyLegacy(): List<Warning> {
        println("🔍 Running legacy verification checks...")
        println()

        verifyViewModels()
        verifyServices()
        verifyRepositories()
        verifyModels()
        verifyLayerDependencies()
        verifyNamingConventions()
        verifyTests()

        return warnings
    }

    private fun verifyViewModels() {
        println("  → Checking ViewModels...")

        val viewModelFiles = findFiles("app/src/main/java", "*ViewModel.kt")

        viewModelFiles.forEach { file ->
            val content = file.readText()
            val fileName = file.relativeTo(projectDir).path

            // Check for Input/Output pattern
            if (!content.contains("sealed interface Input")) {
                warnings.add(Warning(
                    Severity.ERROR,
                    Category.VIEWMODEL_PATTERN,
                    fileName,
                    findLineNumber(content, "class.*ViewModel"),
                    "ViewModel must implement Input/Output pattern with 'sealed interface Input'"
                ))
            }

            if (!content.contains("sealed interface Output")) {
                warnings.add(Warning(
                    Severity.ERROR,
                    Category.VIEWMODEL_PATTERN,
                    fileName,
                    findLineNumber(content, "class.*ViewModel"),
                    "ViewModel must implement Input/Output pattern with 'sealed interface Output'"
                ))
            }

            // Check for State and Effect inside Output
            if (content.contains("sealed interface Output") && !content.contains("data class State")) {
                warnings.add(Warning(
                    Severity.ERROR,
                    Category.VIEWMODEL_PATTERN,
                    fileName,
                    findLineNumber(content, "sealed interface Output"),
                    "Output must contain 'data class State' for UI state binding"
                ))
            }

            if (content.contains("sealed interface Output") && !content.contains("sealed interface Effect")) {
                warnings.add(Warning(
                    Severity.WARNING,
                    Category.VIEWMODEL_PATTERN,
                    fileName,
                    findLineNumber(content, "sealed interface Output"),
                    "Output should contain 'sealed interface Effect' for one-time events"
                ))
            }

            // Check for onEvent method
            if (!content.contains("fun onEvent(")) {
                warnings.add(Warning(
                    Severity.ERROR,
                    Category.VIEWMODEL_PATTERN,
                    fileName,
                    findLineNumber(content, "class.*ViewModel"),
                    "ViewModel must have 'fun onEvent(input: Input)' method"
                ))
            }

            // Check for @HiltViewModel annotation
            if (!content.contains("@HiltViewModel")) {
                warnings.add(Warning(
                    Severity.ERROR,
                    Category.VIEWMODEL_PATTERN,
                    fileName,
                    findLineNumber(content, "class.*ViewModel"),
                    "ViewModel must have @HiltViewModel annotation for dependency injection"
                ))
            }
        }
    }

    private fun verifyServices() {
        println("  → Checking Services...")

        val serviceFiles = findFiles("app/src/main/java", "*Service.kt")
            .filter { !it.path.contains("ApiService") }

        serviceFiles.forEach { file ->
            val fileName = file.relativeTo(projectDir).path

            // Services should be in domain.service package
            if (!fileName.contains("domain/service")) {
                warnings.add(Warning(
                    Severity.WARNING,
                    Category.LAYER_VIOLATION,
                    fileName,
                    1,
                    "Service should be in domain.service package"
                ))
            }
        }
    }

    private fun verifyRepositories() {
        println("  → Checking Repositories...")

        val repoFiles = findFiles("app/src/main/java", "*Repository*.kt")

        repoFiles.forEach { file ->
            val fileName = file.relativeTo(projectDir).path

            // Repository implementations should be in data.repository
            if (fileName.contains("RepositoryImpl") && !fileName.contains("data/repository")) {
                warnings.add(Warning(
                    Severity.ERROR,
                    Category.LAYER_VIOLATION,
                    fileName,
                    1,
                    "Repository implementation must be in data.repository package"
                ))
            }

            // Repository interfaces should be in domain.repository
            if (!fileName.contains("Impl") && !fileName.contains("domain/repository") &&
                !fileName.contains("data/repository")) {
                warnings.add(Warning(
                    Severity.WARNING,
                    Category.LAYER_VIOLATION,
                    fileName,
                    1,
                    "Repository interface should be in domain.repository package"
                ))
            }
        }
    }

    private fun verifyModels() {
        println("  → Checking Models...")

        val modelFiles = findFiles("app/src/main/java", "*model/**/*.kt")

        modelFiles.forEach { file ->
            val content = file.readText()
            val fileName = file.relativeTo(projectDir).path

            // Domain models should not have Android dependencies
            if (fileName.contains("domain/model") && (content.contains("import android.") || content.contains("import androidx."))) {
                    warnings.add(Warning(
                        Severity.ERROR,
                        Category.LAYER_VIOLATION,
                        fileName,
                        findLineNumber(content, "import android|import androidx"),
                        "Domain models must not have Android framework dependencies"
                    ))
                }
        }
    }

    private fun verifyLayerDependencies() {
        println("  → Checking Layer Dependencies...")

        val domainFiles = findFiles("app/src/main/java", "domain/**/*.kt")

        domainFiles.forEach { file ->
            val content = file.readText()
            val fileName = file.relativeTo(projectDir).path

            // Domain layer should not depend on UI or Android
            if (content.contains("import android.") || content.contains("import androidx.")) {
                warnings.add(Warning(
                    Severity.ERROR,
                    Category.LAYER_VIOLATION,
                    fileName,
                    findLineNumber(content, "import android|import androidx"),
                    "Domain layer must not depend on Android framework"
                ))
            }
        }
    }

    private fun verifyNamingConventions() {
        println("  → Checking Naming Conventions...")

        val kotlinFiles = findFiles("app/src/main/java", "**/*.kt")

        kotlinFiles.forEach { file ->
            val content = file.readText()
            val fileName = file.relativeTo(projectDir).path

            // Check for PascalCase class names
            val classRegex = """class\s+([a-z]\w*)""".toRegex()
            classRegex.findAll(content).forEach { match ->
                warnings.add(Warning(
                    Severity.WARNING,
                    Category.NAMING_CONVENTION,
                    fileName,
                    findLineNumber(content, match.value),
                    "Class name '${match.groupValues[1]}' should use PascalCase"
                ))
            }
        }
    }

    private fun verifyTests() {
        println("  → Checking Test Coverage...")

        // Check that ViewModels have tests
        val viewModelFiles = findFiles("app/src/main/java", "*ViewModel.kt")
        viewModelFiles.forEach { file ->
            val testFilePath = file.absolutePath
                .replace("/main/", "/test/")
                .replace(".kt", "Test.kt")
            val testFile = File(testFilePath)

            if (!testFile.exists()) {
                warnings.add(Warning(
                    Severity.WARNING,
                    Category.MISSING_TEST,
                    file.relativeTo(projectDir).path,
                    1,
                    "Missing unit test: Expected test file at ${testFile.relativeTo(projectDir).path}"
                ))
            }
        }

        // Check that Repositories have tests
        val repoFiles = findFiles("app/src/main/java/com/example/arcana/data/repository", "*Repository.kt")
        repoFiles.forEach { file ->
            val testFilePath = file.absolutePath
                .replace("/main/", "/test/")
                .replace(".kt", "Test.kt")
            val testFile = File(testFilePath)

            if (!testFile.exists()) {
                warnings.add(Warning(
                    Severity.WARNING,
                    Category.MISSING_TEST,
                    file.relativeTo(projectDir).path,
                    1,
                    "Missing unit test: Expected test file at ${testFile.relativeTo(projectDir).path}"
                ))
            }
        }
    }

    private fun findFiles(baseDir: String, pattern: String): List<File> {
        val dir = File(projectDir, baseDir)
        if (!dir.exists()) return emptyList()

        val regex = pattern.replace("*", ".*").toRegex()
        return dir.walkTopDown()
            .filter { it.isFile && regex.matches(it.name) }
            .toList()
    }

    private fun findLineNumber(content: String, pattern: String): Int {
        val regex = pattern.toRegex()
        content.lines().forEachIndexed { index, line ->
            if (regex.containsMatchIn(line)) {
                return index + 1
            }
        }
        return 1
    }

    fun generateReport(): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        val sb = StringBuilder()
        sb.appendLine("# Architecture Verification Report")
        sb.appendLine()
        sb.appendLine("**Generated:** $timestamp")
        sb.appendLine()

        if (warnings.isEmpty()) {
            sb.appendLine("✅ **All checks passed!** No violations found.")
            return sb.toString()
        }

        val errorCount = warnings.count { it.severity == Severity.ERROR }
        val warningCount = warnings.count { it.severity == Severity.WARNING }
        val infoCount = warnings.count { it.severity == Severity.INFO }

        sb.appendLine("## Summary")
        sb.appendLine()
        sb.appendLine("| Severity | Count |")
        sb.appendLine("|----------|-------|")
        sb.appendLine("| ❌ ERROR | $errorCount |")
        sb.appendLine("| ⚠️ WARNING | $warningCount |")
        sb.appendLine("| ℹ️ INFO | $infoCount |")
        sb.appendLine("| **TOTAL** | **${warnings.size}** |")
        sb.appendLine()

        // Group by category
        val byCategory = warnings.groupBy { it.category }

        sb.appendLine("## Violations by Category")
        sb.appendLine()

        byCategory.forEach { (category, violations) ->
            sb.appendLine("### ${category.name.replace("_", " ")}")
            sb.appendLine()
            sb.appendLine("**Count:** ${violations.size}")
            sb.appendLine()

            violations.forEach { warning ->
                val icon = when (warning.severity) {
                    Severity.ERROR -> "❌"
                    Severity.WARNING -> "⚠️"
                    Severity.INFO -> "ℹ️"
                }

                sb.appendLine("$icon **${warning.file}:${warning.line}**")
                if (warning.ruleId != null) {
                    sb.appendLine("   Rule: `${warning.ruleId}`")
                }
                sb.appendLine("   ${warning.message}")
                if (warning.suggestion != null) {
                    sb.appendLine()
                    sb.appendLine("   **Suggestion:**")
                    sb.appendLine("   ${warning.suggestion}")
                }
                sb.appendLine()
            }
        }

        return sb.toString()
    }
}
