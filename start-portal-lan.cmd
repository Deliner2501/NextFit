@echo off
setlocal
cd /d "%~dp0"
set "NEXTFIT_MAVEN=mvn.cmd"
where mvn.cmd >nul 2>nul
if errorlevel 1 (
    if not exist "C:\netbeans-30-bin\netbeans\java\maven\bin\mvn.cmd" (
        echo Maven no encontrado. Agrega mvn.cmd al PATH.
        exit /b 1
    )
    set "NEXTFIT_MAVEN=C:\netbeans-30-bin\netbeans\java\maven\bin\mvn.cmd"
)
call "%NEXTFIT_MAVEN%" -q compile org.codehaus.mojo:exec-maven-plugin:3.5.1:java "-Dexec.mainClass=com.nextfit.Main" "-Dexec.args=--portal-lan"
exit /b %ERRORLEVEL%
