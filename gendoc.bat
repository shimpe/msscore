@echo off
setlocal

rem ============================================================================
rem  gendoc.bat  --  Windows helper to regenerate the MSScore HelpSource
rem
rem  Regenerates the MSScore HelpSource *.schelp files from the class sources
rem  using the "whelk" documentation generator (same setup as the Panola quark).
rem
rem  whelk runs from its own virtual environment (see WHELKPY below), which
rem  already has the required dependencies (toml, mako) installed. To recreate
rem  that environment from scratch:
rem      py -m venv D:\Projects\python\whelk\.venv
rem      D:\Projects\python\whelk\.venv\Scripts\python.exe -m pip install toml mako
rem ============================================================================

rem --- Location of the whelk generator on this machine ------------------------
set "WHELK=D:\Projects\python\whelk\whelk.py"

rem --- Python interpreter from whelk's virtual environment --------------------
set "WHELKPY=D:\Projects\python\whelk\.venv\Scripts\python.exe"

rem --- MSScore project root (the folder this script lives in) -----------------
rem  %~dp0 expands to this script's drive+path and ends with a backslash.
set "MSSCORE=%~dp0"

rem --- Sanity check ----------------------------------------------------------
if not exist "%WHELK%" (
    echo ERROR: whelk not found at "%WHELK%"
    echo        Edit the WHELK variable at the top of this script.
    exit /b 1
)
if not exist "%WHELKPY%" (
    echo ERROR: whelk virtual environment not found at "%WHELKPY%"
    echo        Create it with:
    echo            py -m venv D:\Projects\python\whelk\.venv
    echo            D:\Projects\python\whelk\.venv\Scripts\python.exe -m pip install toml mako
    exit /b 1
)

rem --- Remove previously generated help files --------------------------------
echo Removing old help files...
del /Q "%MSSCORE%HelpSource\Classes\*.schelp" >nul 2>&1

rem --- Regenerate help files from the class sources --------------------------
echo Generating help files...
"%WHELKPY%" "%WHELK%" -i "%MSSCORE%Classes\*.sc" -o "%MSSCORE%HelpSource\Classes"

if errorlevel 1 (
    echo.
    echo ERROR: whelk failed. If it complained about a missing module, install
    echo        its dependencies with:
    echo            "%WHELKPY%" -m pip install toml mako
    exit /b 1
)

echo Done.
endlocal
