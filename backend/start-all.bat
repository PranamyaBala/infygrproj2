@echo off
REM =====================================================
REM  HostelHub - Start All Backend Services
REM  Run this from the backend/ directory
REM =====================================================

set JAVA_HOME=C:\Program Files\Java\jdk-18.0.2
set MAVEN_HOME=%~dp0..\tools\apache-maven-3.9.6
set PATH=%MAVEN_HOME%\bin;%JAVA_HOME%\bin;%PATH%

echo.
echo ============================================
echo   HostelHub Backend Services Launcher
echo   MySQL: hostel_*_db (password: pass)
echo   JAVA_HOME = %JAVA_HOME%
echo   MAVEN_HOME = %MAVEN_HOME%
echo ============================================
echo.

REM Verify Maven
call mvn.cmd -version >nul 2>&1
if ERRORLEVEL 1 (
    echo ERROR: Maven not found! Check the tools directory.
    pause
    exit /b 1
)

echo Starting User Service (port 8081)...
start "User Service" cmd /k "cd /d %~dp0user-service && %MAVEN_HOME%\bin\mvn spring-boot:run"

timeout /t 15 /nobreak >nul
echo Starting Room Service (port 8083)...
start "Room Service" cmd /k "cd /d %~dp0room-service && %MAVEN_HOME%\bin\mvn spring-boot:run"

timeout /t 15 /nobreak >nul
echo Starting Booking Service (port 8085)...
start "Booking Service" cmd /k "cd /d %~dp0booking-service && %MAVEN_HOME%\bin\mvn spring-boot:run"

timeout /t 15 /nobreak >nul
echo Starting Notification Service (port 8087)...
start "Notification Service" cmd /k "cd /d %~dp0notification-service && %MAVEN_HOME%\bin\mvn spring-boot:run"

timeout /t 15 /nobreak >nul
echo Starting API Gateway (port 8080)...
start "API Gateway" cmd /k "cd /d %~dp0api-gateway && %MAVEN_HOME%\bin\mvn spring-boot:run"

echo.
echo ============================================
echo   All services starting!
echo   User Service:         http://localhost:8081
echo   Room Service:         http://localhost:8083
echo   Booking Service:      http://localhost:8085
echo   Notification Service: http://localhost:8087
echo   API Gateway:          http://localhost:8080
echo ============================================
echo.
pause
