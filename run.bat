@echo off
SETLOCAL EnableExtensions EnableDelayedExpansion

set "files="

for %%A in (%*) do (
    set files=%%A !files!
)

echo Files: !files!

java -Xms1m -Xmx1024m -jar "D:\Local Disk\Users\User\Documents\TrashProjects\ImageToPdf\build\libs\ImageToPdf.jar" !files!

pause>nul
endlocal
exit