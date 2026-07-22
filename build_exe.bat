@echo off
REM Build CallGuard.exe from callguard_launcher.py using PyInstaller.
REM Double-click this file. First build downloads tools and can take a few minutes.
cd /d "%~dp0"

echo Installing build tools (pyinstaller, pandas, matplotlib)...
python -m pip install --upgrade pyinstaller pandas matplotlib || goto :err

echo.
echo Building CallGuard.exe ...
pyinstaller --onefile --windowed --name CallGuard ^
  --paths analysis --paths google_voice ^
  --hidden-import analyze_calls --hidden-import gvoice_to_csv ^
  --collect-all pandas --collect-all matplotlib ^
  callguard_launcher.py || goto :err

echo.
echo ============================================================
echo  Done. Your app is:  dist\CallGuard.exe
echo  Keep CallGuard.exe inside this CallGuard folder so it can
echo  find the analysis, google_voice and twilio subfolders.
echo ============================================================
pause
exit /b 0

:err
echo.
echo Build failed. Make sure Python is installed and on PATH.
pause
exit /b 1
