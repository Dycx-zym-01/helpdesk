@echo off
setlocal
powershell.exe -NoExit -ExecutionPolicy Bypass -File "%~dp0init-mysql.ps1"
endlocal
