@echo off
setlocal
title SmartDine Auto-Updater — Service Swap Agent

:: ── 0. Self-Elevate to Administrator Rights ─────────────────────────────────
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo Requesting Administrator privileges to update Program Files...
    powershell -Command "Start-Process '%~f0' -Verb RunAs"
    exit /b
)

echo =========================================================
echo   SMARTDINE AUTO-UPDATER  ^|  Silent Service Swap Agent
echo =========================================================
echo.

:: ── 1. Terminate any active graphical runtime windows ──────────────────────
echo [1/6] Terminating active POS interface...
taskkill /F /IM javaw.exe /T >nul 2>&1
echo       Done.

:: ── 2. Stop the background WinSW service ───────────────────────────────────
echo [2/6] Halting background service (smartdine-heart)...
net stop smartdine-heart >nul 2>&1
echo       Done.

:: ── 3. Cooldown delay — release file handles, sockets and ports ────────────
echo [3/6] Waiting for system resources to release (3 seconds)...
timeout /t 3 /nobreak >nul
echo       Done.

:: ── 4. Overwrite the main production binary ─────────────────────────────────
echo [4/6] Swapping application binaries...
if not exist "C:\SmartDine\temp\smartdine-heart.jar" (
    echo       [ERROR] Update binary not found at C:\SmartDine\temp\smartdine-heart.jar
    echo       Aborting update. The existing installation remains intact.
    pause
    exit /b 1
)
copy /Y "C:\SmartDine\temp\smartdine-heart.jar" "C:\Program Files\SurabhiSmartDine\smartdine-heart.jar" >nul
if errorlevel 1 (
    echo       [ERROR] Binary copy failed. Check write permissions on the install directory.
    pause
    exit /b 1
)
echo       Swap complete.

:: ── 5. Clean the temporary workspace ───────────────────────────────────────
echo [5/6] Cleaning temporary file cache...
del /Q "C:\SmartDine\temp\*.*" >nul 2>&1
echo       Done.

:: ── 6. Restart the WinSW service ───────────────────────────────────────────
echo [6/6] Restarting core service...
net start smartdine-heart >nul 2>&1
echo       Service restarted.

if exist "C:\Program Files\SurabhiSmartDine\jre\bin\javaw.exe" (
    start "" "C:\Program Files\SurabhiSmartDine\jre\bin\javaw.exe" -jar "C:\Program Files\SurabhiSmartDine\smartdine-heart.jar"
) else (
    start "" javaw -jar "C:\Program Files\SurabhiSmartDine\smartdine-heart.jar"
)

echo.
echo =========================================================
echo   SmartDine update complete. POS workspace is launching.
echo =========================================================
exit /b 0
