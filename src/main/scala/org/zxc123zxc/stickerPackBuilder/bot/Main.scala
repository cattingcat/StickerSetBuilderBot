package org.zxc123zxc.stickerPackBuilder.bot

import java.nio.file.Paths
import java.util.concurrent.TimeUnit

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn


object Main {
  private val _home = System.getProperty("user.home")
  private val _dir = System.getProperty("user.dir")
  private val _token = "753599151:AAEfm6xhxFUYTc_j81wHoCZLJrZ0Rxvt_Ec"


  def main(args: Array[String]): Unit = {
    p(s"StickerBuilderBot app runs. ~=${_home}; .=${_dir}")

    val dwebpPath = Paths.get(_dir, "binary/dwebp")

    runBot(dwebpPath.toString)
  }


  private def runBot(libWebpPath: String): Unit = {
    val bot = new StickerBuilderBot(_token, libWebpPath)
    val eol = bot.run()

    println("Press [ENTER] to shutdown the bot, it may take a few seconds...")
    StdIn.readLine()

    bot.shutdown()
    Await.result(eol, Duration(3, TimeUnit.SECONDS))
  }
}
