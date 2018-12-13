package org.zxc123zxc.stickerPackBuilder.bot

import java.util.UUID

import com.bot4s.telegram.api._
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.clients.ScalajHttpClient
import com.bot4s.telegram.methods._
import com.bot4s.telegram.models._
import org.zxc123zxc.stickerPackBuilder.webpTransformer.WebpTransformer
import scalaj.http.Http

import scala.concurrent.Future
import scala.util.{Failure, Success}


class StickerBuilderBot(private val _token: String, private val _dwebpPath: String)
  extends TelegramBot
  with Polling
  with Commands {
  import com.bot4s.telegram.marshalling.CirceEncoders._
  import com.bot4s.telegram.marshalling.CirceDecoders._
  import BotMessages._

  private val _botName = "StickerSetBuilderBot"
  private var _state = Map[Long, StickerSetBuilderState]()
  private val _converter = new WebpTransformer(_dwebpPath)

  override val client = new ScalajHttpClient(_token)


  onSticker((msg, sticker) => {
    val chatId = msg.chat.id
    val userId = msg.from.get.id
    val state = _state.applyOrElse[Long, StickerSetBuilderState](chatId, _ => Idle())

    p(s"$userId $chatId : Sticker ${sticker.fileId} received")

    state match {
      case StartedCreation() => send(chatId, chooseNameFirstly)
      case TitleChosen(setTitle) =>
        _state += (chatId -> Loading(state))
        send(chatId, creatingSet(setTitle))

        val f = getFile(sticker.fileId)
          .flatMap(file => getFileBytes(_token, file.filePath.get))
          .flatMap(bytes => Future {_converter.convertToPng(bytes)})
          .flatMap(bytes => createStickerSet(userId, setTitle, bytes))

        f.onComplete {
          case Success((_, setName)) =>
            p("Success! pack created")
            _state += (chatId -> StickerAdd(setName, StickerSetModification()))
            send(chatId, s"${hereYourLink(formatLink(setName))}" +
              "\nContinue adding stickers to set " +
              "\nAnd type /done when you will be finished")
          case Failure(e) =>
            _state += (chatId -> TitleChosen(setTitle))
            p(s"$userId $chatId : Err : ${e.getMessage}")
            send(chatId, s"I'm sorry, server error occurred :( try again")
        }

      case StickerAdd(setName, _) =>
        _state += (chatId -> Loading(state))

        val future = getFile(sticker.fileId)
          .flatMap(file => getFileBytes(_token, file.filePath.get))
          .flatMap(bytes => Future {_converter.convertToPng(bytes)})
          .flatMap(bytes => addStickerToSet(userId, setName, bytes))

        future.onComplete {
          case Success(_) =>
            p(s"$userId $chatId : Succ : sticker added")
            send(chatId, "Added. Already done? Type /done")
            _state += (chatId -> StickerAdd(setName, StickerSetModification()))
          case Failure(e) =>
            p(s"$userId $chatId : Err : ${e.getMessage}")
            send(chatId, s"I'm sorry, server error occurred :(")
            _state += (chatId -> StickerAdd(setName, StickerSetModification()))
        }

      case StartedEditing() =>
        sticker.setName match {
          case Some(name) =>
            _state += (chatId -> StickerAdd(name, StickerSetModification()))
            send(chatId, s"Okay, sticker set chosen. Send files or stickers you want to add")

          case None => send(chatId, s"I'm sorry, I cant recognize this set, try again")
        }

      case Loading(_) => send(chatId, loading)
      case _ => send(chatId, typeCreate)
    }
  })

  onImage((msg, fileId) => {
    val chatId = msg.chat.id
    val userId = msg.from.get.id
    val state = _state.applyOrElse[Long, StickerSetBuilderState](chatId, _ => Idle())

    p(s"$userId $chatId : Image $fileId received")

    state match {
      case StartedCreation() => send(chatId, chooseNameFirstly)
      case TitleChosen(setTitle) =>
        _state += (chatId -> Loading(state))
        send(chatId, creatingSet(setTitle))

        val f = getFile(fileId)
          .flatMap(file => getFileBytes(_token, file.filePath.get))
          //.flatMap(bytes => Future {_converter.convertToPng(bytes)})
          .flatMap(bytes => createStickerSet(userId, setTitle, bytes))

        f.onComplete {
          case Success((_, setName)) =>
            p("Success! pack created")
            _state += (chatId -> StickerAdd(setName, StickerSetModification()))
            send(chatId, s"${hereYourLink(formatLink(setName))}" +
              "\nContinue adding stickers to set " +
              "\nAnd type /done when you will be finished")
          case Failure(e) =>
            p(s"$userId $chatId : Err : ${e.getMessage}")
            send(chatId, s"I'm sorry, server error occurred :(")
        }

      case StickerAdd(setName, _) =>
        _state += (chatId -> Loading(state))

        val future = getFile(fileId)
          .flatMap(file => getFileBytes(_token, file.filePath.get))
          .flatMap(bytes => addStickerToSet(userId, setName, bytes))

        future.onComplete {
          case Success(_) =>
            p(s"$userId $chatId : Succ : file added")
            send(chatId, "Added. Already done? Type /done")
            _state += (chatId -> StickerAdd(setName, StickerSetModification()))
          case Failure(e) =>
            p(s"$userId $chatId : Err : ${e.getMessage}")
            send(chatId, s"I'm sorry, server error occurred :(")
            _state += (chatId -> StickerAdd(setName, StickerSetModification()))
        }

      case Loading(_) => send(chatId, loading)

      case _ => send(chatId, typeCreate)
    }
  })

  onCommand("/start")(msg => {
    val chatId = msg.chat.id
    send(chatId, welcome)
  })

  onCommand("/create")(msg => {
    val chatId = msg.chat.id
    send(chatId, chooseName)
    _state += (chatId -> StartedCreation())
  })

  onCommand("/add")(msg => {
    val chatId = msg.chat.id
    send(chatId, sendAnySticker)
    _state += (chatId -> StartedEditing())
  })

  onCommand("/done")(msg => {
    val chatId = msg.chat.id
    val userId = msg.from.get.id
    val state = _state.applyOrElse[Long, StickerSetBuilderState](chatId, _ => Idle())

    state match {
      case StickerAdd(setName, m) =>
        p(s"$userId $chatId : Success! All stickers attached")
        send(chatId, hereYourLink(formatLink(setName)))
        _state -= chatId
      case StickerRemove(setName, _) =>
        send(chatId, hereYourLink(formatLink(setName)))
        _state -= chatId
      case Loading(_) => send(chatId, loading)
      case _ => send(chatId, typeCreate)
    }
  })

  onMessage(msg => {
    val chatId = msg.chat.id
    val state = _state.applyOrElse[Long, StickerSetBuilderState](chatId, _ => Idle())

    (state, msg.text) match {
      case (StartedCreation(), Some(text)) if text.length < 64 && !text.contains('/') =>
        _state += (chatId -> TitleChosen(text))
        send(chatId, readyToReceive)
      case (StartedCreation(), Some(text)) if !text.contains('/') => send(chatId, invalidTitle)
      case (Idle(), Some(text)) if !text.contains('/') => send(chatId, welcome)
      //case (Idle(), _) => send(chatId, typeCreate)
    }
  })

  private def onSticker(action: (Message, Sticker) => Unit): Unit = {
    onMessage(msg => {
      msg.sticker match {
        case Some(s@Sticker(_, _, _, _, _, Some(_), _, _)) => action(msg, s)
        case _ =>
      }
    })
  }

  private def onImage(action: (Message, String) => Unit): Unit = {
    onMessage(msg => {
      msg.photo match {
        case Some(photo) => action(msg, photo.last.fileId)
        case _ =>
      }
      msg.document match {
        case Some(file) if file.fileName.map(_.toLowerCase.contains(".png")).nonEmpty => action(msg, file.fileId)
        case _ =>
      }
    })
  }

  private def send(chatId: Long, msg: String): Future[Message] = {
    val sendMsgReq = SendMessage(chatId, msg)
    client.sendRequest[Message, SendMessage](sendMsgReq)
  }

  private def createStickerSet(userId: Int, title: String, pngBytes: Array[Byte], fileName: String = "", emojis: String = "\uD83D\uDC31"): Future[(Boolean, String)] = {
    val id = UUID.randomUUID().toString.filter(_.isLetterOrDigit)
    val normTitle = title.filter(_.isLetterOrDigit)
    val name = s"$id${normTitle}_by_${_botName}".takeRight(50)
    val req = CreateNewStickerSet(
      userId,
      name,
      title,
      InputFile(fileName, pngBytes),
      emojis)

    val res = client.sendRequest[Boolean, CreateNewStickerSet](req)

    res.map(r => (r, name))
  }

  private def addStickerToSet(userId: Int, name: String, pngBytes: Array[Byte], fileName: String = "", emojis: String = "\uD83D\uDC31"): Future[Boolean] = {
    val req = AddStickerToSet(
      userId,
      name,
      InputFile(fileName, pngBytes),
      emojis)

    client.sendRequest[Boolean, AddStickerToSet](req)
  }

  private def getFile(id: String): Future[File] = {
    val req = GetFile(id)
    val res = client.sendRequest[File, GetFile](req)
    res
  }

  private def getFileBytes(token: String, filePath: String): Future[Array[Byte]] = Future {
    val uri = s"https://api.telegram.org/file/bot$token/$filePath"
    val res = Http(uri).asBytes
    res.body
  }

  private def formatLink(name: String): String = s"t.me/addstickers/$name"
}
