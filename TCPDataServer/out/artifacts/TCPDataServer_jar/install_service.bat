@echo off && setlocal EnableDelayedExpansion

@echo off

for /f %%j in ("java.exe") do (
    set JAVA_HOME=%%~dp$PATH:j
)

if %JAVA_HOME%.==. (
    @echo java path not found
)

echo %~dp0

sc create "CompilationServer" binPath= "%JAVA_HOME% -jar %~dp0TCPDataServer.jar" start= system
sc start CompilationServer