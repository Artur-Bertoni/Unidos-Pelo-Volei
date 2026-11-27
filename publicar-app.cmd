@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0publicar-app.ps1" %*
pause
