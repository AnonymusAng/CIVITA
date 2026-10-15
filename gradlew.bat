@ECHO OFF
SETLOCAL
SET APP_HOME=%~dp0
IF DEFINED JAVA_HOME GOTO findJavaFromJavaHome
SET JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
IF %ERRORLEVEL% EQU 0 GOTO execute
ECHO ERROR: JAVA_HOME is not set and no 'java' command could be found.
EXIT /B 1
:findJavaFromJavaHome
SET JAVA_EXE=%JAVA_HOME%\bin\java.exe
IF EXIST "%JAVA_EXE%" GOTO execute
ECHO ERROR: JAVA_HOME points to an invalid directory: %JAVA_HOME%
EXIT /B 1
:execute
"%JAVA_EXE%" "-Xmx64m" "-Xms64m" -classpath "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
SET EXIT_CODE=%ERRORLEVEL%
ENDLOCAL & EXIT /B %EXIT_CODE%
