Write-Host "Starting Antigravity Core Java Chat Server..." -ForegroundColor Green
if (-not (Test-Path "target\chat-server.jar")) {
    & .\compile.ps1
}

$javaCmd = "java"
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    if (Test-Path "C:\Users\shail\.vscode\extensions\redhat.java-1.55.0-win32-x64\jre\21.0.11-win32-x86_64\bin\java.exe") {
        $javaCmd = "C:\Users\shail\.vscode\extensions\redhat.java-1.55.0-win32-x64\jre\21.0.11-win32-x86_64\bin\java.exe"
    }
}

& $javaCmd -jar target\chat-server.jar
