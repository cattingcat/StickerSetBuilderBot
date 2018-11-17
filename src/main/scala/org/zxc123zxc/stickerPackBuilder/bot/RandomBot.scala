package org.zxc123zxc.stickerPackBuilder.bot

import java.util.Random

import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.api.{Polling, TelegramBot}
import com.bot4s.telegram.clients.ScalajHttpClient

import scala.util.Try

class RandomBot(val token: String) extends TelegramBot
  with Polling
  with Commands {

  val client = new ScalajHttpClient(token)
  val rng = new Random(System.currentTimeMillis())

  onCommand("coin" or "flip") { implicit msg =>
    reply(if (rng.nextBoolean()) "Head!" else "Tail!")
  }

  onCommand('real | 'double | 'float) { implicit msg =>
    reply(rng.nextDouble().toString)
  }

  onCommand("/die") { implicit msg =>
    reply((rng.nextInt(6) + 1).toString)
  }

  onCommand("random" or "rnd") { implicit msg =>
    withArgs {
      case Seq(Int(n)) if n > 0 =>
        reply(rng.nextInt(n).toString)
      case _ => reply("Invalid argumentヽ(ಠ_ಠ)ノ")
    }
  }

  onCommand('choose | 'pick | 'select) { implicit msg =>
    withArgs { args =>
      replyMd(if (args.isEmpty) "No arguments provided." else args(rng.nextInt(args.size)))
    }
  }


  /* Int(n) extractor */
  object Int { def unapply(s: String): Option[Int] = Try(s.toInt).toOption }
}
