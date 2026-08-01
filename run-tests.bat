@echo off
echo Running Antigravity Chat Unit Tests...

where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    mvn test
) else (
    powershell -ExecutionPolicy Bypass -File .\run-tests.ps1
)
