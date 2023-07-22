@echo off
SETLOCAL EnableExtensions EnableDelayedExpansion

java -Xms1m -Xmx512m -jar "D:\Local Disk\Users\User\Documents\TrashProjects\ImageToPdf\build\libs\ImageToPdf.jar" -c "D:\Local Disk\Users\User\Pictures\halion\halion de frente.png" "D:\Local Disk\Users\User\Pictures\halion\segredos de tank.png" "D:\Local Disk\Users\User\Pictures\Passaporte.jpg"

REM java -Xms1m -Xmx512m -jar "D:\Local Disk\Users\User\Documents\TrashProjects\ImageToPdf\build\libs\ImageToPdf.jar"

endlocal
exit