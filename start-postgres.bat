@echo off
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot
set Path=%JAVA_HOME%\bin;%Path%
set DB_URL=postgresql://moneta_db_etbx_user:nTJ8pdgNoxyNCQK0DoveamhzmYRmD0bQ@dpg-db7p1amk1f9s738hcv7g-a.singapore-postgres.render.com/moneta_db_etbx
echo =============================================
echo  Starting Moneta on Render PostgreSQL DB... 
echo  Target DB: Singapore Render PostgreSQL     
echo  Web App:   http://localhost:8081           
echo =============================================
if exist backend\target\moneta-backend-0.0.1-SNAPSHOT.jar (
    java -jar backend\target\moneta-backend-0.0.1-SNAPSHOT.jar
) else (
    echo Building project first...
    call C:\Users\aadit\tools\apache-maven-3.9.6\bin\mvn.cmd -f backend\pom.xml clean package -DskipTests
    java -jar backend\target\moneta-backend-0.0.1-SNAPSHOT.jar
)
pause
