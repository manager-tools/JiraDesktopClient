@echo off

:: ============================================================================
:: Java options: you can add more options if needed
:: See http://kb.almworks.com/wiki/Deskzilla_Command_Line_Options
:: ============================================================================
set JAVA_OPTIONS=-Xmx400m

:: ============================================================================
:: Set to "yes" if you'd like to have Java console window open
:: ============================================================================
set CONSOLE=no


:: ============================================================================
:: ============================================================================
:: ============================================================================
:: ============================================================================
:: ============================================================================

set PROGRAM_JAR=jiraclient.jar
set PROGRAM_NAME=Client for Jira
set LAUNCHER=launch.bat
set LAUNCH=%~dp0\%LAUNCHER%

if exist "%LAUNCH%" goto run_launcher

echo ==========================================================================
echo ERROR: Cannot start %PROGRAM_NAME%
echo Cannot find %LAUNCHER% in %~dp0
echo ==========================================================================
pause
exit /b

:run_launcher
set X_ALMWORKS_LAUNCH_PERMIT=true
call "%LAUNCH%" %*
