lazy val root = (project in file(".")).
  settings(
    name := "sticker-pack-builder",
    version := "0.23",
    scalaVersion := "2.12.7",
    mainClass in Compile := Some("org.zxc123zxc.sticker-pack-builder.bot.Main")
  )

libraryDependencies += "com.bot4s" %% "telegram-core" % "4.0.0-RC2"
libraryDependencies +=  "org.scalaj" %% "scalaj-http" % "2.4.1"

mainClass in assembly := Some("org.zxc123zxc.sticker-pack-builder.bot.Main")
