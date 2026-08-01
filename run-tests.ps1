Write-Host "Running Antigravity Chat Unit Tests..." -ForegroundColor Yellow

if (Get-Command mvn -ErrorAction SilentlyContinue) {
    mvn test
} else {
    if (-not (Test-Path "target\test-classes")) {
        & .\compile.ps1
    }

    $javaCmd = "java"
    $vscodeJdkJava = "C:\Users\shail\.vscode\extensions\redhat.java-1.55.0-win32-x64\jre\21.0.11-win32-x86_64\bin\java.exe"
    if (Test-Path $vscodeJdkJava) {
        $javaCmd = $vscodeJdkJava
    }

    & $javaCmd -cp "target\classes;target\test-classes" com.chat.test.SimpleUnitTestRunner
}
