Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "Building Antigravity Maven Java Chat Application..." -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

if (Get-Command mvn -ErrorAction SilentlyContinue) {
    Write-Host "[INFO] Executing Maven build (mvn clean package)..." -ForegroundColor Yellow
    mvn clean package -DskipTests
} else {
    Write-Host "[INFO] Maven CLI not detected. Compiling Maven layout using system JDK into target/..." -ForegroundColor Yellow

    $javacPath = "javac"
    $jarPath = "jar"

    $vscodeJdkJavac = "C:\Users\shail\.vscode\extensions\redhat.java-1.55.0-win32-x64\jre\21.0.11-win32-x86_64\bin\javac.exe"
    $vscodeJdkJar = "C:\Users\shail\.vscode\extensions\redhat.java-1.55.0-win32-x64\jre\21.0.11-win32-x86_64\bin\jar.exe"

    if (Test-Path $vscodeJdkJavac) {
        $javacPath = $vscodeJdkJavac
        $jarPath = $vscodeJdkJar
    }

    New-Item -ItemType Directory -Force -Path "target\classes", "target\test-classes" | Out-Null

    $mainJavaFiles = Get-ChildItem -Path "src\main\java" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
    $testJavaFiles = Get-ChildItem -Path "src\test\java" -Recurse -Filter "*.java" | Where-Object { $_.Name -ne "ChatAppTest.java" } | Select-Object -ExpandProperty FullName

    Write-Host "[INFO] Compiling main Java source files..." -ForegroundColor Gray
    & $javacPath -d target\classes $mainJavaFiles

    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Main compilation failed." -ForegroundColor Red
        exit 1
    }

    Write-Host "[INFO] Compiling test Java source files (SimpleUnitTestRunner)..." -ForegroundColor Gray
    & $javacPath -cp target\classes -d target\test-classes $testJavaFiles

    Copy-Item -Path "src\main\resources\schema.sql" -Destination "target\classes\schema.sql" -Force

    Write-Host "[INFO] Packaging target\chat-server.jar..." -ForegroundColor Gray
    & $jarPath --create --file target\chat-server.jar --main-class com.chat.server.ServerMain -C target\classes .

    Write-Host "[INFO] Packaging target\chat-client.jar..." -ForegroundColor Gray
    & $jarPath --create --file target\chat-client.jar --main-class com.chat.client.ClientMain -C target\classes .
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "[SUCCESS] Build completed successfully! Target JARs created in target\" -ForegroundColor Green
} else {
    Write-Host "[ERROR] Build failed." -ForegroundColor Red
}
