@echo off
SETLOCAL EnableExtensions EnableDelayedExpansion

set "files="

for %%A in (%*) do (
    set files=%%A !files!
)

echo Files: !files!

java -Xms1m -Xmx256m -jar "D:\Local Disk\Users\User\Documents\GitHub\WhisperJsonToSrt\build\libs\WhisperJsonToSrt.jar" !files!

pause>nul
endlocal
exit