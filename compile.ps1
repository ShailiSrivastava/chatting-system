Write-Host "Compiling Antigravity Core Java Chat Application..." -ForegroundColor Yellow

if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

$JAVAC_PATH = "C:\Users\shail\.vscode\extensions\redhat.java-1.55.0-win32-x64\jre\21.0.11-win32-x86_64\bin\javac.exe"

& $JAVAC_PATH -cp "lib\h2.jar" -d bin src\com\chat\common\model\*.java src\com\chat\common\protocol\*.java src\com\chat\common\util\*.java src\com\chat\server\config\*.java src\com\chat\server\db\*.java src\com\chat\server\dao\*.java src\com\chat\server\dao\impl\*.java src\com\chat\server\service\*.java src\com\chat\server\network\*.java src\com\chat\server\ServerMain.java src\com\chat\client\network\*.java src\com\chat\client\listener\*.java src\com\chat\client\ui\*.java src\com\chat\client\ClientMain.java src\com\chat\test\*.java

if ($LASTEXITCODE -eq 0) {
    Write-Host "[SUCCESS] Compilation completed cleanly with zero errors! Output stored in bin\" -ForegroundColor Green
} else {
    Write-Host "[ERROR] Compilation failed. Please check compiler errors above." -ForegroundColor Red
}
