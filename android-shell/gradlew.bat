@ECHO OFF
SETLOCAL

SET DIRNAME=%~dp0
IF "%DIRNAME%" == "" SET DIRNAME=.

SET APP_BASE_NAME=%~n0
SET APP_HOME=%DIRNAME%

SET WRAPPER_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
SET WRAPPER_MAIN=org.gradle.wrapper.GradleWrapperMain

IF NOT EXIST "%WRAPPER_JAR%" (
  ECHO Gradle wrapper jar not found at "%WRAPPER_JAR%".
  ECHO Open this project in Android Studio or run "gradle wrapper" to generate wrapper files.
  EXIT /B 1
)

IF "%JAVA_HOME%" == "" (
  SET JAVA_EXE=java
) ELSE (
  SET JAVA_EXE=%JAVA_HOME%\bin\java.exe
)

"%JAVA_EXE%" -classpath "%WRAPPER_JAR%" %WRAPPER_MAIN% %*
ENDLOCAL
