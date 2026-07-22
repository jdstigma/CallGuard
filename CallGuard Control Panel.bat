@echo off
REM Double-click to open the CallGuard Control Panel.
cd /d "%~dp0"
python callguard_launcher.py
if errorlevel 1 pause
