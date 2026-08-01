@echo off
echo Starting Antigravity Core Java Chat Client...
if not exist bin (
    call compile.bat
)
"C:\Users\shail\.vscode\extensions\redhat.java-1.55.0-win32-x64\jre\21.0.11-win32-x86_64\bin\java.exe" -cp "bin;lib\h2.jar" com.chat.client.ClientMain
