## common build
sbt compile
sbt package

## build and run super-jar
sbt assembly
java -jar ~/Desktop/sticker-pack-builder/target/scala-2.12/sticker-pack-builder-assembly-0.23.jar