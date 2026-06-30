# Start AiSLAM RAG API (requires Java 21+ and Maven on PATH, or IntelliJ bundled tools)
$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot

if (-not $env:JAVA_HOME) {
    $ideaJbr = "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.4\jbr"
    if (Test-Path $ideaJbr) {
        $env:JAVA_HOME = $ideaJbr
    }
}

$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mvn) {
    $ideaMvn = "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd"
    if (Test-Path $ideaMvn) { $mvn = $ideaMvn } else { throw "Maven not found. Install Maven or open project in IntelliJ." }
} else {
    $mvn = $mvn.Source
}

Set-Location $projectRoot
Write-Host "Starting RAG API on http://localhost:8080 ..."
& $mvn spring-boot:run
