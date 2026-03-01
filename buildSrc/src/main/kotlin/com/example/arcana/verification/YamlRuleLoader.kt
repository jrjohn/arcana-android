package com.example.arcana.verification

import org.yaml.snakeyaml.Yaml
import java.io.File

/**
 * Loads and parses YAML rule files
 */
@Suppress("UNCHECKED_CAST")
class YamlRuleLoader(private val projectDir: File) {

    private val yaml = Yaml()

    /**
     * Load all rule files from .architecture-verification/rules/
     */
    fun loadAllRules(): List<RuleFile> {
        val rulesDir = File(projectDir, ".architecture-verification/rules")
        if (!rulesDir.exists()) {
            println("⚠️  Rules directory not found: ${rulesDir.absolutePath}")
            return emptyList()
        }

        val ruleFiles = mutableListOf<RuleFile>()

        rulesDir.walkTopDown()
            .filter { it.isFile && it.extension == "yaml" }
            .forEach { file ->
                try {
                    val ruleFile = parseRuleFile(file)
                    ruleFiles.add(ruleFile)
                    println("  ✓ Loaded: ${file.relativeTo(rulesDir).path} (${ruleFile.rules.size} rules)")
                } catch (e: Exception) {
                    println("  ✗ Error loading ${file.name}: ${e.message}")
                }
            }

        return ruleFiles
    }

    /**
     * Load verification configuration
     */
    fun loadConfig(): VerificationConfig? {
        val configFile = File(projectDir, ".architecture-verification/config/verification-config.yaml")
        if (!configFile.exists()) {
            println("⚠️  Config file not found: ${configFile.absolutePath}")
            return null
        }

        return try {
            parseConfig(configFile)
        } catch (e: Exception) {
            println("  ✗ Error loading config: ${e.message}")
            null
        }
    }

    /**
     * Parse a single rule YAML file
     */
    private fun parseRuleFile(file: File): RuleFile {
        val data = yaml.load<Map<String, Any>>(file.inputStream())

        val version = data["version"] as? String ?: "1.0"
        val category = data["category"] as? String ?: "unknown"
        val subcategory = data["subcategory"] as? String
        val description = data["description"] as? String

        val rulesData = data["rules"] as? List<Map<String, Any>> ?: emptyList()
        val rules = rulesData.map { parseRule(it) }

        val metadataData = data["metadata"] as? Map<String, Any>
        val metadata = metadataData?.let { parseMetadata(it) }

        return RuleFile(
            version = version,
            category = category,
            subcategory = subcategory,
            description = description,
            rules = rules,
            metadata = metadata
        )
    }

    /**
     * Parse a single rule from YAML data
     */
    private fun parseRule(data: Map<String, Any>): Rule {
        val id = data["id"] as? String ?: error("Rule missing 'id'")
        val name = data["name"] as? String ?: error("Rule '$id' missing 'name'")
        val severity = data["severity"] as? String ?: "WARNING"
        val enabled = data["enabled"] as? Boolean ?: true
        val description = data["description"] as? String
        val message = data["message"] as? String ?: "Rule violation"
        val suggestion = data["suggestion"] as? String
        val reference = data["reference"] as? String

        val checkData = data["check"] as? Map<String, Any> ?: emptyMap()
        val check = parseCheck(checkData)

        val examplesData = data["examples"] as? Map<String, Any>
        val examples = examplesData?.let { parseExamples(it) }

        return Rule(
            id = id,
            name = name,
            severity = severity,
            enabled = enabled,
            description = description,
            check = check,
            message = message,
            suggestion = suggestion,
            reference = reference,
            examples = examples
        )
    }

    /**
     * Parse check configuration
     */
    private fun parseCheck(data: Map<String, Any>): Check {
        return Check(
            filePattern = data["file_pattern"] as? String,
            mustContain = data["must_contain"],
            mustNotContain = data["must_not_contain"],
            mustContainPattern = data["must_contain_pattern"] as? String,
            withinInterface = data["within_interface"] as? String,
            withinClass = data["within_class"] as? Boolean,
            mustBeInDirectory = data["must_be_in_directory"] as? String,
            excludePatterns = (data["exclude_patterns"] as? List<*>)?.filterIsInstance<String>(),
            classNaming = data["class_naming"] as? String,
            functionNaming = data["function_naming"] as? String,
            requiresTestFile = data["requires_test_file"] as? Boolean,
            testFilePattern = data["test_file_pattern"] as? String,
            testFileLocation = data["test_file_location"] as? String,
            customValidator = data["custom_validator"] as? String,
            maxFileLength = (data["max_file_length"] as? Number)?.toInt(),
            maxFunctionLength = (data["max_function_length"] as? Number)?.toInt(),
            maxParameters = (data["max_parameters"] as? Number)?.toInt(),
            forbiddenImports = (data["forbidden_imports"] as? List<*>)?.filterIsInstance<String>(),
            requiredImports = (data["required_imports"] as? List<*>)?.filterIsInstance<String>()
        )
    }

    /**
     * Parse examples
     */
    private fun parseExamples(data: Map<String, Any>): Examples {
        return Examples(
            good = (data["good"] as? List<*>)?.filterIsInstance<String>(),
            bad = (data["bad"] as? List<*>)?.filterIsInstance<String>(),
            correct = data["correct"] as? String,
            incorrect = data["incorrect"] as? String
        )
    }

    /**
     * Parse metadata
     */
    private fun parseMetadata(data: Map<String, Any>): Metadata {
        return Metadata(
            totalRules = (data["total_rules"] as? Number)?.toInt(),
            lastUpdated = data["last_updated"] as? String,
            author = data["author"] as? String,
            version = data["version"] as? String
        )
    }

    /**
     * Parse verification config
     */
    private fun parseConfig(file: File): VerificationConfig {
        val data = yaml.load<Map<String, Any>>(file.inputStream())

        val globalData = data["global"] as? Map<String, Any>
        val global = globalData?.let { parseGlobalSettings(it) }

        val checksData = data["checks"] as? Map<String, Any>
        val checks = checksData?.mapValues { (_, value) ->
            parseCategoryConfig(value as? Map<String, Any> ?: emptyMap())
        }

        return VerificationConfig(
            global = global,
            checks = checks
        )
    }

    private fun parseGlobalSettings(data: Map<String, Any>): GlobalSettings {
        return GlobalSettings(
            failOnError = data["fail_on_error"] as? Boolean ?: true,
            parallelChecks = data["parallel_checks"] as? Boolean ?: true,
            detailedReports = data["detailed_reports"] as? Boolean ?: true
        )
    }

    private fun parseCategoryConfig(data: Map<String, Any>): CategoryConfig {
        val enabled = data["enabled"] as? Boolean ?: true
        val checksData = data["checks"] as? Map<String, Any>
        val checks = checksData?.mapValues { (_, value) ->
            parseCheckConfig(value as? Map<String, Any> ?: emptyMap())
        }

        return CategoryConfig(
            enabled = enabled,
            checks = checks
        )
    }

    private fun parseCheckConfig(data: Map<String, Any>): CheckConfig {
        return CheckConfig(
            enabled = data["enabled"] as? Boolean ?: true,
            severity = data["severity"] as? String
        )
    }
}
