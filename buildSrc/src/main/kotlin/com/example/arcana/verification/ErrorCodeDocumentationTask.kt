package com.example.arcana.verification

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Gradle task that generates HTML documentation for error codes
 */
open class ErrorCodeDocumentationTask : DefaultTask() {

    init {
        group = "documentation"
        description = "Generates HTML documentation for error codes (ERROR_CODES.html)"
    }

    @TaskAction
    fun generateDocumentation() {
        logger.lifecycle("")
        logger.lifecycle("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        logger.lifecycle("📋 Generating Error Code Documentation...")
        logger.lifecycle("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        logger.lifecycle("")

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())

        // Parse ErrorCode.kt to extract codes
        val errorCodes = parseErrorCodes()

        // Generate HTML
        val htmlContent = generateHtml(errorCodes, timestamp)

        // Write to app directory
        val reportFile = project.file("ERROR_CODES.html")
        reportFile.writeText(htmlContent)
        logger.lifecycle("✅ Documentation generated: ${reportFile.absolutePath}")

        // Copy to project root docs directory
        val projectRoot = if (project.name == "app") {
            project.rootProject.projectDir
        } else {
            project.projectDir
        }
        val docsDir = File(projectRoot, "docs")
        if (!docsDir.exists()) {
            docsDir.mkdirs()
        }
        val docsCopy = File(docsDir, "ERROR_CODES.html")
        docsCopy.writeText(htmlContent)

        logger.lifecycle("✅ Documentation copied to: ${docsCopy.absolutePath}")
        logger.lifecycle("")
    }

    private fun parseErrorCodes(): List<ErrorCodeInfo> {
        val errorCodeFile = project.file("src/main/java/com/example/arcana/core/common/ErrorCode.kt")
        if (!errorCodeFile.exists()) {
            logger.warn("ErrorCode.kt not found, generating empty documentation")
            return emptyList()
        }

        val content = errorCodeFile.readText()
        val codes = mutableListOf<ErrorCodeInfo>()

        // Regex to match: object E1000_NO_CONNECTION : ErrorCode(
        val objectRegex = """object\s+([EW]\d{4}_\w+)\s*:\s*ErrorCode\s*\(\s*"([EW]\d{4})"\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"\s*\)""".toRegex()

        objectRegex.findAll(content).forEach { match ->
            val name = match.groupValues[1]
            val code = match.groupValues[2]
            val description = match.groupValues[3]
            val category = match.groupValues[4]

            codes.add(ErrorCodeInfo(
                name = name,
                code = code,
                description = description,
                category = category,
                isWarning = code.startsWith("W")
            ))
        }

        return codes.sortedBy { it.code }
    }

    private fun generateHtml(codes: List<ErrorCodeInfo>, timestamp: String): String {
        val errors = codes.filter { !it.isWarning }
        val warnings = codes.filter { it.isWarning }

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Error Code Reference - ${project.name}</title>
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
            max-width: 1400px;
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
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .summary-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin: 20px 0;
        }

        .summary-card {
            background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            text-align: center;
        }

        .summary-card .label {
            font-size: 0.9rem;
            color: #666;
            margin-bottom: 8px;
        }

        .summary-card .value {
            font-size: 2rem;
            font-weight: bold;
            color: #667eea;
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

        .code-badge {
            display: inline-block;
            padding: 4px 12px;
            border-radius: 4px;
            font-weight: bold;
            font-family: 'Courier New', monospace;
            font-size: 0.9rem;
        }

        .error-code {
            background: #f8d7da;
            color: #721c24;
        }

        .warning-code {
            background: #fff3cd;
            color: #856404;
        }

        .category-badge {
            display: inline-block;
            padding: 3px 10px;
            border-radius: 12px;
            font-size: 0.8rem;
            font-weight: 600;
        }

        .category-network {
            background: #cfe2ff;
            color: #084298;
        }

        .category-validation {
            background: #f8d7da;
            color: #721c24;
        }

        .category-server {
            background: #fff3cd;
            color: #856404;
        }

        .category-authentication {
            background: #d1ecf1;
            color: #0c5460;
        }

        .category-data {
            background: #d4edda;
            color: #155724;
        }

        .category-database {
            background: #e2e3e5;
            color: #383d41;
        }

        .category-system {
            background: #f8d7da;
            color: #721c24;
        }

        .search-box {
            width: 100%;
            padding: 12px 20px;
            font-size: 1rem;
            border: 2px solid #e0e0e0;
            border-radius: 8px;
            margin-bottom: 20px;
        }

        .search-box:focus {
            outline: none;
            border-color: #667eea;
        }

        .footer {
            background: #f8f9fa;
            padding: 20px 40px;
            text-align: center;
            color: #666;
            border-top: 1px solid #e0e0e0;
        }

        .icon {
            font-size: 1.3rem;
        }
    </style>
    <script>
        function searchCodes() {
            const input = document.getElementById('searchInput');
            const filter = input.value.toUpperCase();
            const tables = document.querySelectorAll('table tbody');

            tables.forEach(tbody => {
                const rows = tbody.getElementsByTagName('tr');
                for (let i = 0; i < rows.length; i++) {
                    const row = rows[i];
                    const text = row.textContent || row.innerText;
                    if (text.toUpperCase().indexOf(filter) > -1) {
                        row.style.display = '';
                    } else {
                        row.style.display = 'none';
                    }
                }
            });
        }
    </script>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>📋 Error Code Reference</h1>
            <p><strong>Project:</strong> ${project.name}</p>
            <p><strong>Generated:</strong> $timestamp</p>
        </div>

        <div class="content">
            <!-- Summary -->
            <div class="section">
                <h2 class="section-title"><span class="icon">📊</span> Summary</h2>
                <div class="summary-grid">
                    <div class="summary-card">
                        <div class="label">Total Codes</div>
                        <div class="value">${codes.size}</div>
                    </div>
                    <div class="summary-card">
                        <div class="label">Error Codes</div>
                        <div class="value">${errors.size}</div>
                    </div>
                    <div class="summary-card">
                        <div class="label">Warning Codes</div>
                        <div class="value">${warnings.size}</div>
                    </div>
                    <div class="summary-card">
                        <div class="label">Categories</div>
                        <div class="value">${codes.map { it.category }.distinct().size}</div>
                    </div>
                </div>
            </div>

            <!-- Search -->
            <div class="section">
                <input type="text" id="searchInput" class="search-box"
                       onkeyup="searchCodes()" placeholder="Search error codes, descriptions, or categories...">
            </div>

            <!-- Error Codes -->
            <div class="section">
                <h2 class="section-title"><span class="icon">❌</span> Error Codes (E####)</h2>
                ${if (errors.isEmpty()) {
                    "<p>No error codes defined.</p>"
                } else {
                    generateTable(errors, isWarning = false)
                }}
            </div>

            <!-- Warning Codes -->
            <div class="section">
                <h2 class="section-title"><span class="icon">⚠️</span> Warning Codes (W####)</h2>
                ${if (warnings.isEmpty()) {
                    "<p>No warning codes defined.</p>"
                } else {
                    generateTable(warnings, isWarning = true)
                }}
            </div>

            <!-- Code Ranges -->
            <div class="section">
                <h2 class="section-title"><span class="icon">📖</span> Code Ranges</h2>
                <table>
                    <caption>Code Ranges</caption>
                    <thead>
                        <tr>
                            <th>Range</th>
                            <th>Category</th>
                            <th>Description</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><span class="code-badge error-code">E1000-E1999</span></td>
                            <td><span class="category-badge category-network">Network</span></td>
                            <td>Network-related errors (connection, timeout, etc.)</td>
                        </tr>
                        <tr>
                            <td><span class="code-badge error-code">E2000-E2999</span></td>
                            <td><span class="category-badge category-validation">Validation</span></td>
                            <td>Input validation errors</td>
                        </tr>
                        <tr>
                            <td><span class="code-badge error-code">E3000-E3999</span></td>
                            <td><span class="category-badge category-server">Server</span></td>
                            <td>Server-side errors (HTTP 4xx, 5xx)</td>
                        </tr>
                        <tr>
                            <td><span class="code-badge error-code">E4000-E4999</span></td>
                            <td><span class="category-badge category-authentication">Authentication</span></td>
                            <td>Authentication and authorization errors</td>
                        </tr>
                        <tr>
                            <td><span class="code-badge error-code">E5000-E5999</span></td>
                            <td><span class="category-badge category-data">Data</span></td>
                            <td>Data conflict and integrity errors</td>
                        </tr>
                        <tr>
                            <td><span class="code-badge error-code">E6000-E6999</span></td>
                            <td><span class="category-badge category-database">Database</span></td>
                            <td>Database operation errors</td>
                        </tr>
                        <tr>
                            <td><span class="code-badge error-code">E9000-E9999</span></td>
                            <td><span class="category-badge category-system">System</span></td>
                            <td>Unknown and system-level errors</td>
                        </tr>
                        <tr>
                            <td><span class="code-badge warning-code">W1000-W1999</span></td>
                            <td><span class="category-badge category-network">Network</span></td>
                            <td>Network-related warnings</td>
                        </tr>
                        <tr>
                            <td><span class="code-badge warning-code">W2000-W2999</span></td>
                            <td><span class="category-badge category-validation">Validation</span></td>
                            <td>Validation warnings</td>
                        </tr>
                        <tr>
                            <td><span class="code-badge warning-code">W3000-W3999</span></td>
                            <td><span class="category-badge category-data">Data</span></td>
                            <td>Data-related warnings</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>

        <div class="footer">
            <p><strong>Generated by:</strong> Error Code Documentation Task</p>
            <p><strong>Timestamp:</strong> $timestamp</p>
        </div>
    </div>
</body>
</html>
        """.trimIndent()
    }

    private fun generateTable(codes: List<ErrorCodeInfo>, isWarning: Boolean): String {
        val codeClass = if (isWarning) "warning-code" else "error-code"
        val caption = if (isWarning) "Warning Codes" else "Error Codes"

        return """
            <table>
                <caption>$caption</caption>
                <thead>
                    <tr>
                        <th>Code</th>
                        <th>Name</th>
                        <th>Category</th>
                        <th>Description</th>
                    </tr>
                </thead>
                <tbody>
                    ${codes.joinToString("") { code ->
                        """
                        <tr>
                            <td><span class="code-badge $codeClass">${code.code}</span></td>
                            <td><code>${code.name}</code></td>
                            <td><span class="category-badge category-${code.category.lowercase()}">${code.category}</span></td>
                            <td>${code.description}</td>
                        </tr>
                        """
                    }}
                </tbody>
            </table>
        """.trimIndent()
    }

    data class ErrorCodeInfo(
        val name: String,
        val code: String,
        val description: String,
        val category: String,
        val isWarning: Boolean
    )
}
