@echo off
setlocal
cd /d "%~dp0"
if not exist out mkdir out
javac -d out src\yep\*.java
if errorlevel 1 (
  echo Build failed. Check that JDK 17 or later is installed.
  pause
  exit /b 1
)
java -cp out yep.WebServer
