@echo off
cd /d "%~dp0"
py -3 -m venv .venv
if errorlevel 1 goto fail
.venv\Scripts\python.exe -m pip install -r requirements.txt
if errorlevel 1 goto fail
echo Kurulum tamamlandi. Kullanim icin README.md dosyasini ac.
pause
exit /b 0
:fail
echo Kurulum tamamlanamadi. Python 3.10-3.12 ve internet baglantisini kontrol et.
pause
exit /b 1
