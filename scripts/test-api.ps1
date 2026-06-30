# AiSLAM RAG API test script (PowerShell)
# Prerequisites: LM Studio server on :1234, Qdrant on :6333, Spring Boot on :8080

$base = "http://localhost:8080"

Write-Host "1. Health check..."
Invoke-RestMethod -Uri "$base/api/health" -Method Get

Write-Host "`n2. Index document..."
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$indexJson = Join-Path $scriptDir "index-sample.json"
$questionJson = Join-Path $scriptDir "question-sample.json"

$indexResult = Invoke-RestMethod -Uri "$base/api/documents/index" -Method Post -InFile $indexJson -ContentType "application/json; charset=utf-8"
$indexResult | ConvertTo-Json

Write-Host "`n3. Retrieve sources..."
$retrieveResult = Invoke-RestMethod -Uri "$base/api/rag/retrieve" -Method Post -InFile $questionJson -ContentType "application/json; charset=utf-8"
$retrieveResult | ConvertTo-Json -Depth 5

Write-Host "`n4. Ask (RAG + LLM)..."
$askResult = Invoke-RestMethod -Uri "$base/api/rag/ask" -Method Post -InFile $questionJson -ContentType "application/json; charset=utf-8"
$askResult | ConvertTo-Json -Depth 5

Write-Host "`nDone."
