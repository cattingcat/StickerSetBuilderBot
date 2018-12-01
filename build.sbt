import sbt.Keys.mainClass

val mainPath = "org.zxc123zxc.stickerPackBuilder.bot.Main"

lazy val root = (project in file(".")).
  settings(
    name := "sticker-pack-builder",
    version := "0.23",
    scalaVersion := "2.12.7",
    mainClass in Compile := Some(mainPath),
    mainClass in assembly := Some(mainPath),
    assemblyJarName in assembly := "bot.jar"
  )

libraryDependencies += "com.bot4s" %% "telegram-core" % "4.0.0-RC2"
libraryDependencies +=  "org.scalaj" %% "scalaj-http" % "2.4.1"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5" % "test"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"



lazy val stage = taskKey[Unit]("Stage task for Heroku")
stage := {
  assembly.value
}



