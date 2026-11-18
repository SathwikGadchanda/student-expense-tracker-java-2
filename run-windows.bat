@echo off
cd /d "%~dp0"
if exist "campus-lost-found.jar" (
  java -jar "campus-lost-found.jar" %*
) else (
  if not exist "target\campus-lost-found.jar" call mvn clean verify
  if not exist "target\campus-lost-found.jar" exit /b 1
  java -jar "target\campus-lost-found.jar" %*
)
if errorlevel 1 pause
