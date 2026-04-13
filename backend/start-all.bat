@echo off
REM =====================================================
REM  HostelHub - Start Backend Monolith
REM  Run this from the backend/ directory
REM =====================================================

set JAVA_HOME=C:\Program Files\Java\jdk-18.0.2
set MAVEN_HOME=%~dp0..\tools\apache-maven-3.9.6
set PATH=%MAVEN_HOME%\bin;%JAVA_HOME%\bin;%PATH%

echo.
echo ============================================
echo   HostelHub Backend Monolith Launcher
echo   MySQL: hostel_db
echo   JAVA_HOME = %JAVA_HOME%
echo   MAVEN_HOME = %MAVEN_HOME%
echo ============================================
echo.

REM Verify Maven
call mvn.cmd -version >nul 2>&1
if ERRORLEVEL 1 (
    echo ERROR: Maven not found! Using global mvn...
    start "Backend Monolith" cmd /k "cd /d %~dp0 && mvn spring-boot:run"
    exit /b 0
)

echo Starting Monolithic Backend (port 8080)...
start "Backend Monolith" cmd /k "cd /d %~dp0 && %MAVEN_HOME%\bin\mvn spring-boot:run"

echo.
echo ============================================
echo   Backend is starting!
echo   Main Application:     http://localhost:8080
echo ============================================
echo.
pause
