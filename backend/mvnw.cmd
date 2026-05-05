@echo off
setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.\

set "MAVEN_PROJECTBASEDIR=%DIRNAME%"
set "WRP_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set "WRP_CLASS=org.apache.maven.wrapper.MavenWrapperMain"

@REM Find Java
if not "%JAVA_HOME%" == "" (
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
  set JAVA_EXE=java.exe
)

if not exist "%JAVA_EXE%" (
  echo Error: JAVA_HOME is not set correctly.
  exit /b 1
)

@REM Run Maven
"%JAVA_EXE%" -classpath "%WRP_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" %WRP_CLASS% %*
