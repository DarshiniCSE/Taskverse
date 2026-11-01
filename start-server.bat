@echo off
set "CATALINA_HOME=C:\Users\darsh\Downloads\apache-tomcat-10.1.61-windows-x64\apache-tomcat-10.1.61"
set "JAVA_HOME=C:\Program Files\Java\jdk-26.0.1"

echo ========================================================
echo              STARTING TASKVERSE SERVER
echo ========================================================
echo Tomcat Path : %CATALINA_HOME%
echo Java Path   : %JAVA_HOME%
echo.

call "%CATALINA_HOME%\bin\startup.bat"

echo.
echo Server window launched!
echo Open your browser at:
echo http://localhost:8080/TaskVerse/
echo ========================================================
pause
