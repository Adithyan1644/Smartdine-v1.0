@echo off
:: Self-healing native updater loop for Windows SmartDine deployment.

echo ===================================================
echo [SmartDine Updater] Starting application update...
echo ===================================================

:: 1. Stop the Windows background service
echo Stopping SmartDine Heart Service...
net stop smartdine-heart > nul 2>&1
taskkill /f /im javaw.exe > nul 2>&1

:: 2. Grace period for locks to release
echo Waiting for file locks to release...
timeout /t 3 /nobreak > nul

:: 3. Overwrite jar if new update file is present
if exist "%~dp0new-smartdine-heart.jar" (
    echo Swapping to new application binary...
    copy /y "%~dp0new-smartdine-heart.jar" "%~dp0smartdine-heart.jar" > nul
    del "%~dp0new-smartdine-heart.jar" > nul
) else (
    echo Warning: new-smartdine-heart.jar not found. Skipping overwrite.
)

:: 4. Cleanup temporary files
echo Cleaning up update temporary files...
if exist "%~dp0temp_download" (
    rmdir /s /q "%~dp0temp_download" > nul 2>&1
)

:: 5. Restart the Windows service
echo Starting SmartDine Heart Service...
net start smartdine-heart > nul

echo ===================================================
echo [SmartDine Dynamic Updater] Success! Update completed.
echo ===================================================
