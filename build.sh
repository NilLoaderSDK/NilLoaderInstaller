#!/usr/bin/env sh
set -eu
rm -rf build-manual dist
mkdir -p build-manual/classes dist
find src/main/java -name '*.java' > build-manual/sources.txt
javac --release 17 -encoding UTF-8 -d build-manual/classes @build-manual/sources.txt
jar --create --file dist/NilLoaderInstaller.jar --main-class me.tamkungz.nilloaderinstaller.Main -C build-manual/classes .
echo "Built dist/NilLoaderInstaller.jar"
