@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed me on an "AS IS" BASIS, WITHOUT WARRANTIES
@REM OR CONDITIONS OF ANY KIND, either express or implied.         
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM ----------------------------------------------------------------------------

@echo off
setlocal

set MAVEN_CMD_LINE_ARGS=%*

if not "%JAVA_HOME%" == "" goto setJavaHome
for %%i in (java.exe) do set "JAVACMD=%%~$PATH:i"
if not "%JAVACMD%" == "" goto checkWithJavaCmd
echo Error: JAVA_HOME is not set and no 'java' command could be found in your PATH.
goto error

:setJavaHome
set "JAVACMD=%JAVA_HOME%\bin\java.exe"
if exist "%JAVACMD%" goto checkWithJavaCmd
echo Error: JAVA_HOME is set to an invalid directory: "%JAVA_HOME%"
goto error

:checkWithJavaCmd
where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    mvn %MAVEN_CMD_LINE_ARGS%
    exit /b %ERRORLEVEL%
)

echo [INFO] Running project using VS Code / System Java environment...
if exist "C:\Users\shail\.vscode\extensions\redhat.java-1.55.0-win32-x64\jre\21.0.11-win32-x86_64\bin\javac.exe" (
    set "JAVACMD=C:\Users\shail\.vscode\extensions\redhat.java-1.55.0-win32-x64\jre\21.0.11-win32-x86_64\bin\java.exe"
)

mvn %MAVEN_CMD_LINE_ARGS%
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven execution failed or 'mvn' command was not found. Please install Maven or add it to PATH.
)
exit /b %ERRORLEVEL%

:error
exit /b 1
