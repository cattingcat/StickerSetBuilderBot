package org.zxc123zxc.stickerPackBuilder.console

object Main {
  private val _home = System.getProperty("user.home")
  private val _dir = System.getProperty("user.dir")

  def main(args: Array[String]): Unit = {
    println(s"Console app. ~=${_home}; .=${_dir}")
  }
}
