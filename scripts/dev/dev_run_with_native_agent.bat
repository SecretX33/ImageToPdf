@echo off
SETLOCAL EnableExtensions

set "JAR_PATH=build\libs\ImageToPdf.jar"

cd ..
cd ..

if not exist "%JAR_PATH%" (
    echo App .jar was not found, executing 'shadowJar' task
    .\gradlew shadowJar
)

java -agentlib:native-image-agent=config-output-dir=./src/main/resources/META-INF/native-image -jar "%JAR_PATH%" --jpg-quality 0.85 --resize 0.65 --sort modified_date "D:\Local Disk\Users\User\Pictures\halion\halion de frente.png" "D:\Local Disk\Users\User\Pictures\halion\segredos de tank.png" -i

pause
endlocal