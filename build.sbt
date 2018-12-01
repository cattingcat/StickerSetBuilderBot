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

//lazy val subProj = (project in file("."))
//  .settings(
//    name := "sticker-pack-builder-sub-project",
//    version := "0.2",
//    scalaVersion := "2.12.7",
//    mainClass in assembly := Some("org.zxc123zxc.stickerPackBuilder.console.Main"),
//    assemblyJarName in assembly := "console.jar"
//  )


libraryDependencies += "com.bot4s" %% "telegram-core" % "4.0.0-RC2"
libraryDependencies +=  "org.scalaj" %% "scalaj-http" % "2.4.1"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5" % "test"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

// https://mvnrepository.com/artifact/org.jcodec/jcodec
libraryDependencies += "org.jcodec" % "jcodec" % "0.2.3"




lazy val stage = taskKey[Unit]("Stage task for Heroku")
stage := {
  assembly.value
}



