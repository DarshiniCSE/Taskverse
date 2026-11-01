@echo off
set "CATALINA_HOME=C:\Users\darsh\Downloads\apache-tomcat-10.1.61-windows-x64\apache-tomcat-10.1.61"
set "JAVA_HOME=C:\Program Files\Java\jdk-26.0.1"

echo ========================================================
echo              STOPPING TASKVERSE SERVER
echo ========================================================
call "%CATALINA_HOME%\bin\shutdown.bat"
echo Server stopped.
pause
