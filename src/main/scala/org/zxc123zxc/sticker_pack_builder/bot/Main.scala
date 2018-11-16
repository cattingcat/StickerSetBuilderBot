package org.zxc123zxc.sticker_pack_builder.bot

import java.util.concurrent.TimeUnit

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {
  private val home = System.getProperty("user.home")

  def main(args: Array[String]): Unit = {
    println(s"StickerBuilderBot app runs $home")

    runBot()
  }

  private def runBot(): Unit = {
    val bot = new StickerBuilderBot("")

    val eol = bot.run()
    println("Press [ENTER] to shutdown the bot, it may take a few seconds...")
    scala.io.StdIn.readLine()
    bot.shutdown()
    Await.result(eol, Duration(3, TimeUnit.SECONDS))
  }
}
