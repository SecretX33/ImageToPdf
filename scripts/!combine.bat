@echo off
SETLOCAL EnableExtensions EnableDelayedExpansion

set "files="
set "IMAGE_TO_PDF_JAR={{SET_THE_PATH_HERE}}\ImageToPdf.jar"

for %%A in (%*) do (
    set files=%%A !files!
)

java -Xms1m -Xmx1g -jar "%IMAGE_TO_PDF_JAR%" --combine --resize 0.75 --jpg-quality 0.7 --sort name !files!

endlocal
exit