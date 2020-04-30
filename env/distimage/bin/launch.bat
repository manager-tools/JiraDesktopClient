:: Application launcher - do not call manually.
:: --------------------------------------------
@echo off

if "%X_ALMWORKS_LAUNCH_PERMIT%" == "true" goto run
echo ==========================================================================
echo ERROR: %~nx0 should not be called manually. 
echo Please start application with other .bat files.
echo ==========================================================================
pause
exit /b

:run
if "%PROGRAM_JAR%" == "" goto bad_call
if "%PROGRAM_NAME%" == "" goto bad_call

set BIN=%~dp0
set HOME=%BIN%\..
set JAVA_EXE=javaw.exe
set START_OPTIONS=/b

if "%CONSOLE%" == "no" goto no_console
set JAVA_EXE=java.exe
set START_OPTIONS=
:no_console

if not exist "%HOME%\%PROGRAM_JAR%" goto no_deskzilla

if "%JAVA_HOME%" == "" goto find_java_1
set JAVA=%JAVA_HOME%\bin\%JAVA_EXE%
if not exist "%JAVA%" set JAVA=%JAVA_HOME%\jre\bin\%JAVA_EXE%
if exist "%JAVA%" goto launch

:find_java_1
if not exist "%HOME%\jre\bin\%JAVA_EXE%" goto find_java_2
set JAVA=%HOME%\jre\bin\%JAVA_EXE%
goto launch

:find_java_2
set JAVA=%JAVA_EXE%
goto launch

:: ============================================================================
:no_deskzilla
echo ==========================================================================
echo ERROR: Cannot start %PROGRAM_NAME%
echo Cannot find %PROGRAM_JAR% in %HOME%.
echo ==========================================================================
pause
exit /b

:bad_call
echo ==========================================================================
echo ERROR: Bad call to %~nx0
echo [%PROGRAM_JAR%] [%PROGRAM_NAME%]
echo ==========================================================================
pause
exit /b


:: ============================================================================
:launch
start "%PROGRAM_NAME% Console" %START_OPTIONS% "%JAVA%" %JAVA_OPTIONS% -jar "%HOME%\%PROGRAM_JAR%" %*
