Write-Host "Running Antigravity Chat Unit Tests..." -ForegroundColor Cyan
if (-not (Test-Path "bin")) {
    & ".\compile.ps1"
}
& "C:\Users\shail\.vscode\extensions\redhat.java-1.55.0-win32-x64\jre\21.0.11-win32-x86_64\bin\java.exe" -cp "bin;lib\h2.jar" com.chat.test.SimpleUnitTestRunner
