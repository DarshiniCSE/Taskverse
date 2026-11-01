# TaskVerse - Automated Build and Deployment Script (Windows PowerShell)
# Target Environment: Java 26, Apache Tomcat 10.1, MySQL 8.0

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "       TASKVERSE - BUILD AND DEPLOYMENT PIPELINE        " -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Resolve Project and Tool Directories
$ProjectRoot = $PSScriptRoot
if (-not $ProjectRoot) { $ProjectRoot = (Get-Location).Path }

$TomcatHome = "C:\Users\darsh\Downloads\apache-tomcat-10.1.61-windows-x64\apache-tomcat-10.1.61"
$JavaCompiler = "C:\Program Files\Java\jdk-26.0.1\bin\javac.exe"

# Verify Prerequisites
if (-not (Test-Path $JavaCompiler)) {
    $foundJavac = (Get-Command javac.exe -ErrorAction SilentlyContinue)
    if ($foundJavac) {
        $JavaCompiler = $foundJavac.Source
    } else {
        Write-Error "Java compiler (javac.exe) not found. Please ensure JDK 26 is installed."
        exit 1
    }
}
Write-Host "[OK] Using Java Compiler : $JavaCompiler" -ForegroundColor Green

if (-not (Test-Path $TomcatHome)) {
    Write-Error "Tomcat 10.1 not found at: $TomcatHome"
    exit 1
}
Write-Host "[OK] Using Tomcat Home   : $TomcatHome" -ForegroundColor Green

# 2. Prepare Directory Layout
$WebInfDir    = Join-Path $ProjectRoot "webapp\WEB-INF"
$ClassesDir   = Join-Path $WebInfDir "classes"
$LibDir       = Join-Path $WebInfDir "lib"
$DeployTarget = Join-Path $TomcatHome "webapps\TaskVerse"

if (-not (Test-Path $ClassesDir)) {
    New-Item -ItemType Directory -Force -Path $ClassesDir | Out-Null
}
if (-not (Test-Path $LibDir)) {
    New-Item -ItemType Directory -Force -Path $LibDir | Out-Null
}

# 3. Copy MySQL JDBC Driver to WEB-INF/lib
$TomcatDriver = Join-Path $TomcatHome "lib\mysql-connector-j-9.2.0.jar"
$LocalDriver  = Join-Path $LibDir "mysql-connector-j-9.2.0.jar"

if (Test-Path $TomcatDriver) {
    Copy-Item -Path $TomcatDriver -Destination $LocalDriver -Force
    Write-Host "[OK] Configured MySQL JDBC Driver: $LocalDriver" -ForegroundColor Green
} else {
    Write-Host "[!] Warning: mysql-connector-j not found in Tomcat lib." -ForegroundColor Yellow
}

# 4. Copy Database Configuration File
$LocalDbProp = Join-Path $ProjectRoot "db\db.properties"
$TargetDbProp = Join-Path $ClassesDir "db.properties"

if (Test-Path $LocalDbProp) {
    Copy-Item -Path $LocalDbProp -Destination $TargetDbProp -Force
    Write-Host "[OK] Copied database configuration to: $TargetDbProp" -ForegroundColor Green
} else {
    $ExampleDbProp = Join-Path $ProjectRoot "db\db.properties.example"
    Copy-Item -Path $ExampleDbProp -Destination $TargetDbProp -Force
    Write-Host "[!] Created default db.properties in WEB-INF/classes." -ForegroundColor Yellow
}

# 5. Compile Java Source Files
Write-Host ""
Write-Host "[*] Compiling Java source files with javac..." -ForegroundColor White

$ServletApi = Join-Path $TomcatHome "lib\servlet-api.jar"
$Classpath = "$ServletApi;$LocalDriver;$ClassesDir"

$SourceDir = Join-Path $ProjectRoot "src"
$SourceFiles = (Get-ChildItem -Path $SourceDir -Filter "*.java" -Recurse).FullName

if (-not $SourceFiles) {
    Write-Error "No Java source files found in src directory."
    exit 1
}

$count = $SourceFiles.Count
Write-Host "    Found $count Java source file(s) to compile."

# Compile
& $JavaCompiler -encoding UTF-8 -cp $Classpath -d $ClassesDir $SourceFiles
if ($LASTEXITCODE -ne 0) {
    Write-Error "Java compilation failed with exit code $LASTEXITCODE."
    exit $LASTEXITCODE
}
Write-Host "[OK] Compilation completed successfully into: $ClassesDir" -ForegroundColor Green

# 6. Deploy to Tomcat webapps/TaskVerse
Write-Host ""
Write-Host "[*] Deploying web application to Tomcat..." -ForegroundColor White

if (-not $DeployTarget.EndsWith("TaskVerse")) {
    Write-Error "Security Halt: Target path must be TaskVerse. Aborting to protect other apps."
    exit 1
}

if (-not (Test-Path $DeployTarget)) {
    New-Item -ItemType Directory -Force -Path $DeployTarget | Out-Null
}

$WebappSource = Join-Path $ProjectRoot "webapp"
Copy-Item -Path "$WebappSource\*" -Destination $DeployTarget -Recurse -Force
$DeployedWebXml = Join-Path $DeployTarget "WEB-INF\web.xml"
if (Test-Path $DeployedWebXml) {
    (Get-Item $DeployedWebXml).LastWriteTime = Get-Date
}
Write-Host "[OK] Deployed TaskVerse application to: $DeployTarget" -ForegroundColor Green

Write-Host ""
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "                 DEPLOYMENT COMPLETE!                   " -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host " 1. Create your MySQL database using 'db/schema.sql' in MySQL Workbench." -ForegroundColor White
Write-Host " 2. Verify your password in 'db/db.properties'." -ForegroundColor White
Write-Host " 3. Start Tomcat:  cmd /c `"$TomcatHome\bin\startup.bat`"" -ForegroundColor White
Write-Host " 4. Open in Chrome: http://localhost:8080/TaskVerse/" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""
