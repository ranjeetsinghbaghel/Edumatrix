@echo off
echo Stopping all Python and Uvicorn processes...
taskkill /F /IM python.exe
taskkill /F /IM uvicorn.exe
echo All processes stopped. You can now start the server in a new terminal.
pause
