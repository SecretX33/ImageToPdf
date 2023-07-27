@echo off
SETLOCAL EnableExtensions EnableDelayedExpansion

set "files="
set "IMAGE_TO_PDF_JAR={{SET_THE_PATH_HERE}}\ImageToPdf.jar"

for %%A in (%*) do (
    set files=%%A !files!
)

java -Xms1m -Xmx1g -jar "%IMAGE_TO_PDF_JAR%" --resize 0.8 --jpg-quality 0.75 --sort name !files!

endlocal
exit