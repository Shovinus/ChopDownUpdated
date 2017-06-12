./gradlew clean
./gradlew build
ZP3=$(zipgrep "\"mcversion\":" build/libs/modid-1.0.jar)
MCVER=$(echo $ZP3 | grep -o '[0-9]\+\([.][0-9]\+\)*' | head -1)
mkdir bin
cp build/libs/modid-1.0.jar bin/chopdown-last-$MCVER.jar
