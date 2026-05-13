@echo off
setlocal
if not "%COMMANDLINE_TOOL_DIR%"=="" if exist "%COMMANDLINE_TOOL_DIR%\\command-line-tools\\bin\\hvigorw.bat" (
  call "%COMMANDLINE_TOOL_DIR%\\command-line-tools\\bin\\hvigorw.bat" %*
  exit /b %ERRORLEVEL%
)

if not "%HARMONY_CLI_DIR%"=="" if exist "%HARMONY_CLI_DIR%\\command-line-tools\\bin\\hvigorw.bat" (
  call "%HARMONY_CLI_DIR%\\command-line-tools\\bin\\hvigorw.bat" %*
  exit /b %ERRORLEVEL%
)

node "%~dp0hvigor-wrapper.js" %*
endlocal
