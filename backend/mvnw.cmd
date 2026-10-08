@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script (only-script distribution).
@REM Requires a JDK on PATH or JAVA_HOME set. Downloads Maven on first use.
@REM ----------------------------------------------------------------------------
@echo off
setlocal

set MAVEN_WRAPPER_DIST_VERSION=3.9.9
set WRAPPER_DIR=%~dp0.mvn\wrapper
set MAVEN_HOME_LOCAL=%WRAPPER_DIR%\apache-maven-%MAVEN_WRAPPER_DIST_VERSION%
set DIST_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_WRAPPER_DIST_VERSION%/apache-maven-%MAVEN_WRAPPER_DIST_VERSION%-bin.zip

if exist "%MAVEN_HOME_LOCAL%\bin\mvn.cmd" goto run

echo Downloading Apache Maven %MAVEN_WRAPPER_DIST_VERSION% ...
if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%WRAPPER_DIR%\maven.zip'; ^
   Expand-Archive -Path '%WRAPPER_DIR%\maven.zip' -DestinationPath '%WRAPPER_DIR%' -Force; ^
   Remove-Item '%WRAPPER_DIR%\maven.zip'"

:run
call "%MAVEN_HOME_LOCAL%\bin\mvn.cmd" %*
endlocal
