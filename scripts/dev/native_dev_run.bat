@echo off
SETLOCAL EnableExtensions

set "EXE_PATH=build\libs\ImageToPdf.exe"

cd ..
cd ..

if not exist "%EXE_PATH%" (
    echo Could not find application compiled binary, please make sure the app is compiled before running this script
    pause>nul
    goto END
)

.\%EXE_PATH% --jpg-quality 0.85 --resize 0.65 --sort modified_date "D:\Local Disk\Users\User\Pictures\halion\halion de frente.png" "D:\Local Disk\Users\User\Pictures\halion\segredos de tank.png"

:END
endlocal
