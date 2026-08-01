@echo off
echo ==================================================
echo Building Antigravity Maven Java Chat Application...
echo ==================================================

where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [INFO] Executing Maven build...
    mvn clean package -DskipTests
) else (
    echo [INFO] Maven not found in PATH. Using JDK to compile Maven layout into target\ ...
    powershell -ExecutionPolicy Bypass -File .\compile.ps1
)
