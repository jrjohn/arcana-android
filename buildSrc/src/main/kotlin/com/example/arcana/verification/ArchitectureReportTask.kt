package com.example.arcana.verification

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Gradle task that generates a detailed architecture verification report
 */
open class ArchitectureReportTask : DefaultTask() {

    init {
        group = "verification"
        description = "Generates detailed architecture verification report (ARCHITECTURE_VERIFICATION.html)"
    }

    @TaskAction
    fun generateReport() {
        logger.lifecycle("")
        logger.lifecycle("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        logger.lifecycle("📊 Generating Architecture Verification Report...")
        logger.lifecycle("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        logger.lifecycle("")

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        val analysis = analyzeProject()

        // Generate HTML report
        val htmlReport = generateHtmlReport(analysis, timestamp)

        // Write HTML report to app directory
        val reportFile = project.file("ARCHITECTURE_VERIFICATION_REPORT.html")
        reportFile.writeText(htmlReport)

        logger.lifecycle("✅ HTML Report generated: ${reportFile.absolutePath}")

        // Copy HTML report to project root docs directory
        val projectRoot = if (project.name == "app") {
            project.rootProject.projectDir
        } else {
            project.projectDir
        }
        val docsDir = File(projectRoot, "docs")
        if (!docsDir.exists()) {
            docsDir.mkdirs()
        }
        val htmlDocsCopy = File(docsDir, "ARCHITECTURE_VERIFICATION.html")
        htmlDocsCopy.writeText(htmlReport)

        logger.lifecycle("✅ HTML Report copied to: ${htmlDocsCopy.absolutePath}")
        logger.lifecycle("")
    }

    private fun generateHtmlReport(analysis: ProjectAnalysis, timestamp: String): String {
        val warnings = collectBuildWarnings()
        val todos = findTodos()
        val recommendations = generateRecommendations(analysis)

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Architecture Verification Report - ${project.name}</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
            line-height: 1.6;
            color: #333;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 20px;
        }

        .container {
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            border-radius: 12px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
            overflow: hidden;
        }

        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 40px;
            text-align: center;
        }

        .header h1 {
            font-size: 2.5rem;
            margin-bottom: 10px;
            text-shadow: 2px 2px 4px rgba(0,0,0,0.2);
        }

        .header p {
            font-size: 1.1rem;
            opacity: 0.9;
        }

        .content {
            padding: 40px;
        }

        .section {
            margin-bottom: 40px;
        }

        .section-title {
            font-size: 1.8rem;
            color: #667eea;
            margin-bottom: 20px;
            padding-bottom: 10px;
            border-bottom: 3px solid #667eea;
        }

        .subsection-title {
            font-size: 1.3rem;
            color: #764ba2;
            margin: 20px 0 10px 0;
        }

        table {
            width: 100%;
            border-collapse: collapse;
            margin: 20px 0;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            border-radius: 8px;
            overflow: hidden;
        }

        th {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 15px;
            text-align: left;
            font-weight: 600;
        }

        td {
            padding: 12px 15px;
            border-bottom: 1px solid #f0f0f0;
        }

        tr:nth-child(even) {
            background-color: #f8f9fa;
        }

        tr:hover {
            background-color: #e9ecef;
        }

        .metric-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin: 20px 0;
        }

        .metric-card {
            background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            text-align: center;
        }

        .metric-card .label {
            font-size: 0.9rem;
            color: #666;
            margin-bottom: 8px;
        }

        .metric-card .value {
            font-size: 2rem;
            font-weight: bold;
            color: #667eea;
        }

        .layer-card {
            background: #f8f9fa;
            padding: 20px;
            border-radius: 8px;
            margin: 15px 0;
            border-left: 4px solid #667eea;
        }

        .layer-card h4 {
            color: #667eea;
            margin-bottom: 10px;
        }

        .status-badge {
            display: inline-block;
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 0.85rem;
            font-weight: 600;
            margin: 2px;
        }

        .status-pass {
            background: #d4edda;
            color: #155724;
        }

        .status-fail {
            background: #f8d7da;
            color: #721c24;
        }

        .priority-high {
            background: #f8d7da;
            color: #721c24;
            padding: 4px 12px;
            border-radius: 4px;
            font-weight: bold;
            display: inline-block;
        }

        .priority-medium {
            background: #fff3cd;
            color: #856404;
            padding: 4px 12px;
            border-radius: 4px;
            font-weight: bold;
            display: inline-block;
        }

        .priority-low {
            background: #d1ecf1;
            color: #0c5460;
            padding: 4px 12px;
            border-radius: 4px;
            font-weight: bold;
            display: inline-block;
        }

        .recommendation-card {
            background: #fff;
            border: 1px solid #e0e0e0;
            border-radius: 8px;
            padding: 20px;
            margin: 15px 0;
            box-shadow: 0 2px 4px rgba(0,0,0,0.05);
        }

        .recommendation-card h4 {
            color: #333;
            margin-bottom: 10px;
        }

        .recommendation-card p {
            color: #666;
            white-space: pre-line;
        }

        ul {
            margin: 10px 0 10px 20px;
        }

        li {
            margin: 5px 0;
            color: #555;
        }

        .footer {
            background: #f8f9fa;
            padding: 20px 40px;
            text-align: center;
            color: #666;
            border-top: 1px solid #e0e0e0;
        }

        .checkmark {
            font-size: 1.2em;
        }

        .icon-success {
            color: #28a745;
        }

        .icon-error {
            color: #dc3545;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>📊 Architecture Verification Report</h1>
            <p><strong>Project:</strong> ${project.name}</p>
            <p><strong>Generated:</strong> $timestamp</p>
        </div>

        <div class="content">
            <!-- Executive Summary -->
            <div class="section">
                <h2 class="section-title">Executive Summary</h2>
                <div class="metric-grid">
                    <div class="metric-card">
                        <div class="label">Total Kotlin Files</div>
                        <div class="value">${analysis.totalFiles}</div>
                    </div>
                    <div class="metric-card">
                        <div class="label">ViewModels</div>
                        <div class="value">${analysis.viewModels.size}</div>
                    </div>
                    <div class="metric-card">
                        <div class="label">Services</div>
                        <div class="value">${analysis.services.size}</div>
                    </div>
                    <div class="metric-card">
                        <div class="label">Repositories</div>
                        <div class="value">${analysis.repositories.size}</div>
                    </div>
                    <div class="metric-card">
                        <div class="label">Test Files</div>
                        <div class="value">${analysis.testFiles.size}</div>
                    </div>
                    <div class="metric-card">
                        <div class="label">Lines of Code</div>
                        <div class="value">${String.format("%,d", analysis.totalLinesOfCode)}</div>
                    </div>
                </div>
            </div>

            <!-- ViewModel Pattern Compliance -->
            <div class="section">
                <h2 class="section-title">ViewModel Pattern Compliance</h2>
                ${if (analysis.viewModels.isEmpty()) {
                    "<p>No ViewModels found.</p>"
                } else {
                    """
                    <table>
                        <caption>ViewModel Pattern Compliance</caption>
                        <thead>
                            <tr>
                                <th>ViewModel</th>
                                <th>Input</th>
                                <th>Output.State</th>
                                <th>Output.Effect</th>
                                <th>onEvent()</th>
                                <th>@HiltViewModel</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${analysis.viewModels.joinToString("") { vm ->
                                """
                                <tr>
                                    <td><strong>${vm.name}</strong></td>
                                    <td>${vm.hasInput.toHtmlCheckmark()}</td>
                                    <td>${vm.hasState.toHtmlCheckmark()}</td>
                                    <td>${vm.hasEffect.toHtmlCheckmark()}</td>
                                    <td>${vm.hasOnEvent.toHtmlCheckmark()}</td>
                                    <td>${vm.hasHiltAnnotation.toHtmlCheckmark()}</td>
                                </tr>
                                """
                            }}
                        </tbody>
                    </table>
                    """
                }}
            </div>

            <!-- Architecture Layers -->
            <div class="section">
                <h2 class="section-title">Architecture Layers</h2>

                <div class="layer-card">
                    <h4>🎨 UI Layer</h4>
                    <ul>
                        <li>Files: ${analysis.uiFiles}</li>
                        <li>ViewModels: ${analysis.viewModels.size}</li>
                    </ul>
                </div>

                <div class="layer-card">
                    <h4>🎯 Domain Layer</h4>
                    <ul>
                        <li>Files: ${analysis.domainFiles}</li>
                        <li>Services: ${analysis.services.size}</li>
                        <li>Zero Android Dependencies: ${analysis.domainHasAndroidDeps.not().toHtmlCheckmark()}</li>
                    </ul>
                </div>

                <div class="layer-card">
                    <h4>💾 Data Layer</h4>
                    <ul>
                        <li>Files: ${analysis.dataFiles}</li>
                        <li>Repositories: ${analysis.repositories.size}</li>
                    </ul>
                </div>
            </div>

            <!-- Build Warnings -->
            <div class="section">
                <h2 class="section-title">Build Warnings</h2>
                ${if (warnings.isEmpty()) {
                    "<p class='icon-success'><span class='checkmark'>✅</span> No warnings detected</p>"
                } else {
                    """
                    <p><strong>Total Warnings: ${warnings.size}</strong></p>
                    <ul>
                        ${warnings.joinToString("") { "<li>$it</li>" }}
                    </ul>
                    """
                }}
            </div>

            <!-- Pending Tasks -->
            <div class="section">
                <h2 class="section-title">Pending Tasks in Codebase</h2>
                ${if (todos.isEmpty()) {
                    "<p class='icon-success'><span class='checkmark'>✅</span> No pending tasks found</p>"
                } else {
                    """
                    <p><strong>Total pending tasks: ${todos.size}</strong></p>
                    <ul>
                        ${todos.joinToString("") { "<li><code>${it.file}:${it.line}</code> - ${it.text}</li>" }}
                    </ul>
                    """
                }}
            </div>

            <!-- Recommendations -->
            <div class="section">
                <h2 class="section-title">Recommendations</h2>
                ${if (recommendations.isEmpty()) {
                    "<p class='icon-success'><span class='checkmark'>✅</span> No recommendations - excellent architecture compliance!</p>"
                } else {
                    recommendations.joinToString("") { rec ->
                        """
                        <div class="recommendation-card">
                            <h4>${rec.title}</h4>
                            <p><strong>Priority:</strong> <span class="priority-${rec.priority.lowercase()}">${rec.priority}</span></p>
                            <p>${rec.description}</p>
                        </div>
                        """
                    }
                }}
            </div>
        </div>

        <div class="footer">
            <p><strong>Report Generated by:</strong> Architecture Verification Plugin</p>
            <p><strong>Timestamp:</strong> $timestamp</p>
        </div>
    </div>
</body>
</html>
        """.trimIndent()
    }

    private fun Boolean.toHtmlCheckmark(): String {
        return if (this) {
            "<span class='checkmark icon-success'>✅</span>"
        } else {
            "<span class='checkmark icon-error'>❌</span>"
        }
    }

    private fun analyzeProject(): ProjectAnalysis {
        // Determine correct base paths
        val srcBasePath = if (project.name == "app") {
            "src/main/java"
        } else {
            "app/src/main/java"
        }
        val testBasePath = if (project.name == "app") {
            "src/test"
        } else {
            "app/src/test"
        }

        val allKotlinFiles = findKotlinFiles(srcBasePath)
        val testFiles = findKotlinFiles(testBasePath)

        val viewModels = allKotlinFiles
            .filter { it.name.endsWith("ViewModel.kt") }
            .map { analyzeViewModel(it) }
            .filter { !it.isAbstract } // Exclude abstract base ViewModels

        val services = allKotlinFiles
            .filter { it.name.contains("Service") && it.path.contains("/domain/") }

        val repositories = allKotlinFiles
            .filter { it.name.endsWith("Repository.kt") && it.path.contains("/data/") }

        val uiFiles = allKotlinFiles.count { it.path.contains("/ui/") }
        val domainFiles = allKotlinFiles.count { it.path.contains("/domain/") }
        val dataFiles = allKotlinFiles.count { it.path.contains("/data/") }

        val domainHasAndroidDeps = checkDomainForAndroidDependencies()

        val totalLinesOfCode = allKotlinFiles.sumOf { countLines(it) }

        return ProjectAnalysis(
            totalFiles = allKotlinFiles.size,
            viewModels = viewModels,
            services = services,
            repositories = repositories,
            testFiles = testFiles,
            uiFiles = uiFiles,
            domainFiles = domainFiles,
            dataFiles = dataFiles,
            domainHasAndroidDeps = domainHasAndroidDeps,
            totalLinesOfCode = totalLinesOfCode
        )
    }

    private fun analyzeViewModel(file: File): ViewModelInfo {
        val content = file.readText()
        val isAbstract = content.contains("abstract class") ||
                        content.contains("abstract\nclass") ||
                        content.contains("abstract  class")
        return ViewModelInfo(
            name = file.nameWithoutExtension,
            hasInput = content.contains("sealed interface Input"),
            hasState = content.contains("data class State"),
            hasEffect = content.contains("sealed interface Effect"),
            hasOnEvent = content.contains("fun onEvent(input: Input)"),
            hasHiltAnnotation = content.contains("@HiltViewModel"),
            isAbstract = isAbstract
        )
    }

    private fun checkDomainForAndroidDependencies(): Boolean {
        val srcBasePath = if (project.name == "app") {
            "src/main/java"
        } else {
            "app/src/main/java"
        }
        val domainFiles = findKotlinFiles(srcBasePath)
            .filter { it.path.contains("/domain/") }

        return domainFiles.any { file ->
            val content = file.readText()
            content.contains("import android.") ||
            content.contains("import androidx.") ||
            content.contains("import kotlinx.android")
        }
    }

    private fun collectBuildWarnings(): List<String> {
        // This is a placeholder - in practice, you'd parse build output
        // For now, return known warnings from the report
        return listOf(
            "app/src/test/.../AppErrorTest.kt:101 - Check for instance is always 'true'"
        )
    }

    private fun findTodos(): List<TodoItem> {
        val todos = mutableListOf<TodoItem>()
        val srcMainPath = if (project.name == "app") {
            "src/main"
        } else {
            "app/src/main"
        }
        val resPath = if (project.name == "app") {
            "src/main/res"
        } else {
            "app/src/main/res"
        }
        val allFiles = findKotlinFiles(srcMainPath) + findXmlFiles(resPath)

        allFiles.forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                if (line.contains("TODO", ignoreCase = true)) {
                    val todoText = line.trim().removePrefix("//").removePrefix("<!--").removeSuffix("-->").trim()
                    todos.add(TodoItem(
                        file = file.relativeTo(project.projectDir).path,
                        line = index + 1,
                        text = todoText
                    ))
                }
            }
        }

        return todos
    }

    private fun generateRecommendations(analysis: ProjectAnalysis): List<Recommendation> { // NOSONAR kotlin:S3776
        val recommendations = mutableListOf<Recommendation>()

        // Check for ViewModels not following pattern
        analysis.viewModels.forEach { vm ->
            if (!vm.hasInput || !vm.hasState || !vm.hasEffect || !vm.hasOnEvent) {
                recommendations.add(Recommendation(
                    title = "${vm.name} - Missing Input/Output Pattern Components",
                    priority = "HIGH",
                    description = buildString {
                        append("The ViewModel is missing required components:\n")
                        if (!vm.hasInput) append("- Missing 'sealed interface Input'\n")
                        if (!vm.hasState) append("- Missing 'data class State'\n")
                        if (!vm.hasEffect) append("- Missing 'sealed interface Effect'\n")
                        if (!vm.hasOnEvent) append("- Missing 'fun onEvent(input: Input)'\n")
                    }
                ))
            }
            if (!vm.hasHiltAnnotation) {
                recommendations.add(Recommendation(
                    title = "${vm.name} - Missing @HiltViewModel",
                    priority = "MEDIUM",
                    description = "ViewModel should use @HiltViewModel annotation for dependency injection"
                ))
            }
        }

        // Check for Android dependencies in domain layer
        if (analysis.domainHasAndroidDeps) {
            recommendations.add(Recommendation(
                title = "Domain Layer Has Android Dependencies",
                priority = "HIGH",
                description = "The domain layer should have zero Android framework dependencies for better testability and separation of concerns"
            ))
        }

        return recommendations
    }

    private fun findKotlinFiles(path: String): List<File> {
        val dir = project.file(path)
        if (!dir.exists()) return emptyList()
        return dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private fun findXmlFiles(path: String): List<File> {
        val dir = project.file(path)
        if (!dir.exists()) return emptyList()
        return dir.walkTopDown().filter { it.isFile && it.extension == "xml" }.toList()
    }

    private fun countLines(file: File): Int = file.readLines().size

    private fun Boolean.toCheckmark() = if (this) "✅" else "❌" // NOSONAR kotlin:S1144

    data class ProjectAnalysis(
        val totalFiles: Int,
        val viewModels: List<ViewModelInfo>,
        val services: List<File>,
        val repositories: List<File>,
        val testFiles: List<File>,
        val uiFiles: Int,
        val domainFiles: Int,
        val dataFiles: Int,
        val domainHasAndroidDeps: Boolean,
        val totalLinesOfCode: Int
    )

    data class ViewModelInfo(
        val name: String,
        val hasInput: Boolean,
        val hasState: Boolean,
        val hasEffect: Boolean,
        val hasOnEvent: Boolean,
        val hasHiltAnnotation: Boolean,
        val isAbstract: Boolean = false
    )

    data class TodoItem(
        val file: String,
        val line: Int,
        val text: String
    )

    data class Recommendation(
        val title: String,
        val priority: String,
        val description: String
    )
}
