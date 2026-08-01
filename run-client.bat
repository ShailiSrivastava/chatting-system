@echo off
echo Starting Antigravity Core Java Chat Client...
if not exist target\chat-client.jar (
    call compile.bat
)

set JAVA_CMD=java
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    if exist "C:\Users\shail\.vscode\extensions\redhat.java-1.55.0-win32-x64\jre\21.0.11-win32-x86_64\bin\java.exe" (
        set JAVA_CMD="C:\Users\shail\.vscode\extensions\redhat.java-1.55.0-win32-x64\jre\21.0.11-win32-x86_64\bin\java.exe"
    )
)

%JAVA_CMD% -jar target\chat-client.jar
