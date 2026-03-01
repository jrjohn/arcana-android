package com.example.arcana.verification

import java.io.File

/**
 * Executes verification rules against source files
 */
class RuleEngine(private val projectDir: File) {

    data class Violation(
        val ruleId: String,
        val severity: String,
        val category: String,
        val file: String,
        val line: Int,
        val message: String,
        val suggestion: String? = null
    )

    private val violations = mutableListOf<Violation>()

    /**
     * Execute all rules and collect violations
     */
    fun executeRules(ruleFiles: List<RuleFile>): List<Violation> {
        violations.clear()

        ruleFiles.forEach { ruleFile ->
            ruleFile.rules.forEach { rule ->
                if (rule.enabled) {
                    executeRule(rule, ruleFile.category)
                }
            }
        }

        return violations
    }

    /**
     * Execute a single rule
     */
    private fun executeRule(rule: Rule, category: String) {
        val check = rule.check

        // Get files matching the pattern
        val files = findMatchingFiles(check.filePattern ?: return)

        files.forEach { file ->
            checkFile(file, rule, category)
        }
    }

    /**
     * Check a single file against a rule
     */
    private fun checkFile( // NOSONAR kotlin:S3776file: File, rule: Rule, category: String) {
        val content = try {
            file.readText()
        } catch (e: Exception) {
            return
        }

        val check = rule.check
        val fileName = file.relativeTo(projectDir).path

        // Check if file should be excluded
        if (shouldExclude(fileName, check.excludePatterns)) {
            return
        }

        // Check directory location
        if (check.mustBeInDirectory != null) {
            val pattern = check.mustBeInDirectory.replace("**/", "").replace("/**", "")
            if (!fileName.contains(pattern)) {
                addViolation(rule, category, fileName, 1)
                return
            }
        }

        // Check must_contain patterns
        check.getMustContainList().forEach { pattern ->
            if (!content.contains(pattern)) {
                val line = findRelevantLine(content, file.name)
                addViolation(rule, category, fileName, line)
            }
        }

        // Check must_not_contain patterns
        check.getMustNotContainList().forEach { pattern ->
            if (content.contains(pattern)) {
                val line = findLineContaining(content, pattern)
                addViolation(rule, category, fileName, line)
            }
        }

        // Check must_contain_pattern (regex)
        if (check.mustContainPattern != null) {
            val regex = check.mustContainPattern.toRegex()
            if (!regex.containsMatchIn(content)) {
                val line = findRelevantLine(content, file.name)
                addViolation(rule, category, fileName, line)
            }
        }

        // Check forbidden imports
        check.forbiddenImports?.forEach { forbidden ->
            val importPattern = "import $forbidden"
            if (content.contains(importPattern)) {
                val line = findLineContaining(content, importPattern)
                addViolation(rule, category, fileName, line)
            }
        }

        // Check required imports
        check.requiredImports?.forEach { required ->
            val importPattern = "import $required"
            if (!content.contains(importPattern)) {
                addViolation(rule, category, fileName, 1)
            }
        }

        // Check class naming
        if (check.classNaming != null) {
            checkClassNaming(content, check.classNaming, rule, category, fileName)
        }

        // Check function naming
        if (check.functionNaming != null) {
            checkFunctionNaming(content, check.functionNaming, rule, category, fileName)
        }

        // Check if test file is required
        if (check.requiresTestFile == true) {
            checkTestFile(file, check.testFilePattern ?: "*Test.kt", rule, category, fileName)
        }

        // Check file length
        if (check.maxFileLength != null) {
            val lineCount = content.lines().size
            if (lineCount > check.maxFileLength) {
                addViolation(rule, category, fileName, 1)
            }
        }
    }

    /**
     * Check class naming convention
     */
    private fun checkClassNaming(
        content: String,
        pattern: String,
        rule: Rule,
        category: String,
        fileName: String
    ) {
        val classRegex = """class\s+(\w+)""".toRegex()
        val namingRegex = pattern.toRegex()

        classRegex.findAll(content).forEach { match ->
            val className = match.groupValues[1]
            if (!namingRegex.matches(className)) {
                val line = findLineContaining(content, "class $className")
                addViolation(rule, category, fileName, line)
            }
        }
    }

    /**
     * Check function naming convention
     */
    private fun checkFunctionNaming(
        content: String,
        pattern: String,
        rule: Rule,
        category: String,
        fileName: String
    ) {
        val functionRegex = """fun\s+(\w+)""".toRegex()
        val namingRegex = pattern.toRegex()

        functionRegex.findAll(content).forEach { match ->
            val functionName = match.groupValues[1]
            if (!namingRegex.matches(functionName)) {
                val line = findLineContaining(content, "fun $functionName")
                addViolation(rule, category, fileName, line)
            }
        }
    }

    /**
     * Check if test file exists
     */
    private fun checkTestFile(
        sourceFile: File,
        @Suppress("UNUSED_PARAMETER") testPattern: String,
        rule: Rule,
        category: String,
        fileName: String
    ) {
        val testFilePath = sourceFile.absolutePath
            .replace("/main/", "/test/")
            .replace(".kt", "Test.kt")

        val testFile = File(testFilePath)
        if (!testFile.exists()) {
            addViolation(rule, category, fileName, 1)
        }
    }

    /**
     * Find files matching a pattern
     */
    private fun findMatchingFiles(pattern: String): List<File> {
        val baseDir = if (projectDir.name == "app") {
            File(projectDir, "src/main/java")
        } else {
            File(projectDir, "app/src/main/java")
        }

        if (!baseDir.exists()) {
            return emptyList()
        }

        // Convert glob pattern to regex
        val regexPattern = pattern
            .replace("**", ".*")
            .replace("*", "[^/]*")
            .replace(".", "\\.")
            .toRegex()

        return baseDir.walkTopDown()
            .filter { it.isFile }
            .filter { file ->
                val relativePath = file.relativeTo(baseDir).path
                regexPattern.matches(relativePath)
            }
            .toList()
    }

    /**
     * Check if file should be excluded
     */
    private fun shouldExclude(fileName: String, excludePatterns: List<String>?): Boolean {
        if (excludePatterns == null) return false

        return excludePatterns.any { pattern ->
            val regex = pattern
                .replace("**", ".*")
                .replace("*", "[^/]*")
                .replace(".", "\\.")
                .toRegex()
            regex.matches(fileName)
        }
    }

    /**
     * Find line number containing a pattern
     */
    private fun findLineContaining(content: String, pattern: String): Int {
        content.lines().forEachIndexed { index, line ->
            if (line.contains(pattern)) {
                return index + 1
            }
        }
        return 1
    }

    /**
     * Find a relevant line for the file
     */
    private fun findRelevantLine(content: String, @Suppress("UNUSED_PARAMETER") fileName: String): Int {
        // Try to find class definition
        val classMatch = """class\s+\w+""".toRegex().find(content)
        if (classMatch != null) {
            return findLineContaining(content, classMatch.value)
        }

        // Try to find interface definition
        val interfaceMatch = """interface\s+\w+""".toRegex().find(content)
        if (interfaceMatch != null) {
            return findLineContaining(content, interfaceMatch.value)
        }

        return 1
    }

    /**
     * Add a violation to the list
     */
    private fun addViolation(rule: Rule, category: String, fileName: String, line: Int) {
        violations.add(
            Violation(
                ruleId = rule.id,
                severity = rule.severity,
                category = category,
                file = fileName,
                line = line,
                message = rule.message,
                suggestion = rule.suggestion
            )
        )
    }
}
