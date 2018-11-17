## common build
sbt compile
sbt package

## build and run super-jar
sbt assembly
./run.sh

## Heroku
heroku ps                   - list of processes
heroku ps:stop run.3836     - stop dyno
heroku run ls               - run custom command