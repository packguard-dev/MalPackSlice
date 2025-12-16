# JSCodeSlicing Setup Script

Write-Host "Starting JSCodeSlicing Setup..." -ForegroundColor Cyan

# 1. Check Java
Write-Host "`n[1/4] Checking Java..."
try {
    $javaVersion = java -version 2>&1 | Out-String
    if ($javaVersion) {
        Write-Host "Java is installed." -ForegroundColor Green
    } else {
        Write-Host "Java is NOT installed or not in PATH. Please install JDK 21+." -ForegroundColor Red
    }
} catch {
    Write-Host "Java check failed." -ForegroundColor Red
}

# 2. Check SBT
Write-Host "`n[2/4] Checking SBT..."
try {
    $sbtVersion = sbt sbtVersion 2>&1 | Select-Object -Last 1
    if ($sbtVersion) {
        Write-Host "SBT is installed." -ForegroundColor Green
    } else {
        Write-Host "SBT is NOT installed or not in PATH." -ForegroundColor Red
    }
} catch {
    Write-Host "SBT check failed." -ForegroundColor Red
}

# 3. Install astgen
Write-Host "`n[3/4] Checking astgen..."
$astgenDir = Join-Path $PSScriptRoot "astgen"
$astgenPath = Join-Path $astgenDir "astgen-win.exe"
$astgenUrl = "https://github.com/joernio/astgen/releases/download/v3.35.0/astgen-win.exe"

if (-not (Test-Path $astgenPath)) {
    Write-Host "astgen binary not found. Downloading..." -ForegroundColor Yellow
    if (-not (Test-Path $astgenDir)) {
        New-Item -ItemType Directory -Path $astgenDir | Out-Null
    }
    
    try {
        Invoke-WebRequest -Uri $astgenUrl -OutFile $astgenPath
        Write-Host "astgen downloaded successfully." -ForegroundColor Green
    } catch {
        Write-Host "Failed to download astgen: $_" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "astgen binary already exists." -ForegroundColor Green
}

# 4. Configure Environment Variable
Write-Host "`n[4/4] Configuring Environment Variables..."
$currentEnv = [System.Environment]::GetEnvironmentVariable('ASTGEN_BIN', 'User')

if ($currentEnv -ne $astgenPath) {
    Write-Host "Setting ASTGEN_BIN environment variable..." -ForegroundColor Yellow
    [System.Environment]::SetEnvironmentVariable('ASTGEN_BIN', $astgenPath, 'User')
    $env:ASTGEN_BIN = $astgenPath
    Write-Host "ASTGEN_BIN set to: $astgenPath" -ForegroundColor Green
    Write-Host "Note: You may need to restart your terminal/IDE for the change to persist globally." -ForegroundColor Yellow
} else {
    Write-Host "ASTGEN_BIN is already configured correctly." -ForegroundColor Green
}

Write-Host "`nSetup Complete! You can now run tests with: sbt test" -ForegroundColor Cyan
