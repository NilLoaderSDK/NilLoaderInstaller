@echo off
setlocal
if exist build-manual rmdir /s /q build-manual
if exist dist rmdir /s /q dist
mkdir build-manual\classes
mkdir dist
dir /s /b src\main\java\*.java > build-manual\sources.txt
javac --release 17 -encoding UTF-8 -d build-manual\classes @build-manual\sources.txt
if errorlevel 1 exit /b 1
jar --create --file dist\NilLoaderInstaller.jar --main-class me.tamkungz.nilloaderinstaller.Main -C build-manual\classes .
if errorlevel 1 exit /b 1
echo Built dist\NilLoaderInstaller.jar
