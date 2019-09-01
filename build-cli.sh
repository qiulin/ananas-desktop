#!/bin/sh

# clean up
rm runner/build/libs/ananas-*.jar
rm cli/ananas-cli*.jar
rm -rf artifacts

mkdir -p artifacts/engine/flink
mkdir -p artifacts/engine/spark
mkdir -p artifacts/engine/dataflow
mkdir -p artifacts/extension

# build cli
if [ -z "$1" ]
then
    echo "use default jdk to build CLI"
    ./gradlew :runner:shadowJar -Dtarget=cli -Prelease=true
else
    echo "use jdk '$1' to build CLI"
    ./gradlew :runner:shadowJar -Dorg.gradle.java.home=$1 -Dtarget=cli -Prelease=true
fi

cp runner/build/libs/ananas-cli*.jar ./cli
cp $(ls ./cli/*.jar) ./cli/ananas-cli-latest.jar

# build artifacts
./gradlew :runner:extensionJar -Prelease=true
./gradlew :runner:engineJar -Prelease=true -Dengine=flink
./gradlew :runner:engineJar -Prelease=true -Dengine=spark
./gradlew :runner:engineJar -Prelease=true -Dengine=dataflow 

cp ./runner/build/libs/ananas-flink* ./artifacts/engine/flink
cp $(ls ./runner/build/libs/ananas-flink*.jar) ./runner/build/libs/ananas-flink-latest.jar
cp ./runner/build/libs/ananas-spark* ./artifacts/engine/spark
cp $(ls ./runner/build/libs/ananas-spark*.jar) ./runner/build/libs/ananas-spark-latest.jar
cp ./runner/build/libs/ananas-dataflow* ./artifacts/engine/dataflow
cp $(ls ./runner/build/libs/ananas-dataflow*.jar) ./runner/build/libs/ananas-dataflow-latest.jar
