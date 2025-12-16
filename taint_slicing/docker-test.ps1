# Docker Test Script for JSCodeSlicing
# Professional malware detection testing in isolated container environment

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "JSCodeSlicing Docker Malware Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Build Docker image
Write-Host "[1/2] Building Docker image..." -ForegroundColor Yellow
docker build -t jscodeslicing-test .

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Docker build failed!" -ForegroundColor Red
    exit 1
}

Write-Host "Successfully built Docker image" -ForegroundColor Green
Write-Host ""

# Run application in container with malware isolation
Write-Host "[2/2] Running malware analysis..." -ForegroundColor Yellow
Write-Host "  - Downloading 10 real malware samples inside container" -ForegroundColor Gray
Write-Host "  - Executing JSCodeSlicing analysis (sbt run)" -ForegroundColor Gray
Write-Host "  - Exporting analysis results" -ForegroundColor Gray
Write-Host ""

# Create container and copy results after execution
$containerId = docker create jscodeslicing-test
docker start -a $containerId
$exitCode = $LASTEXITCODE

# Copy output results from container
Write-Host ""
Write-Host "Exporting analysis results..." -ForegroundColor Yellow
if (Test-Path "./output") {
    Remove-Item -Recurse -Force "./output"
}
docker cp "${containerId}:/app/src/main/resources/output" "./output" 2>$null
docker rm $containerId >$null 2>&1

if ($exitCode -ne 0) {
    Write-Host "ERROR: Analysis failed!" -ForegroundColor Red
    exit 1
}

if (Test-Path "./output") {
    Write-Host "Analysis results exported to ./output directory" -ForegroundColor Green
    Get-ChildItem "./output" -Recurse | Select-Object Name, Length
}
else {
    Write-Host "WARNING: Output directory not found" -ForegroundColor Yellow
}

Write-Host "Analysis completed successfully" -ForegroundColor Green
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Malware samples safely isolated in container" -ForegroundColor Green
Write-Host "No malware persists on host machine" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
