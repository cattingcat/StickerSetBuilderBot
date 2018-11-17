package org.zxc123zxc.stickerPackBuilder.bot

import java.util.concurrent.TimeUnit

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn


object Main {
  private val _home = System.getProperty("user.home")
  private val _dir = System.getProperty("user.dir")
  private val _token = "753599151:AAEfm6xhxFUYTc_j81wHoCZLJrZ0Rxvt_Ec"


  def main(args: Array[String]): Unit = {
    println(s"StickerBuilderBot app runs. ~=${_home}; .=${_dir}")

    val uri = getClass.getClassLoader.getResource("dwebp-mac")
    val dwebpPath = uri.getPath

    runBot(dwebpPath)
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
