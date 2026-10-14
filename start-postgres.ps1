# Moneta Local Launcher with Render PostgreSQL DB
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:DB_URL = "postgresql://moneta_db_etbx_user:nTJ8pdgNoxyNCQK0DoveamhzmYRmD0bQ@dpg-db7p1amk1f9s738hcv7g-a.singapore-postgres.render.com/moneta_db_etbx"

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host " Starting Moneta on Render PostgreSQL DB... " -ForegroundColor Cyan
Write-Host " Target DB: Singapore Render PostgreSQL     " -ForegroundColor Cyan
Write-Host " Web App:   http://localhost:8081           " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

if (Test-Path "backend\target\moneta-backend-0.0.1-SNAPSHOT.jar") {
    java -jar "backend\target\moneta-backend-0.0.1-SNAPSHOT.jar"
} else {
    Write-Host "Building project first..." -ForegroundColor Yellow
    & "C:\Users\aadit\tools\apache-maven-3.9.6\bin\mvn.cmd" -f backend\pom.xml clean package -DskipTests
    java -jar "backend\target\moneta-backend-0.0.1-SNAPSHOT.jar"
}
