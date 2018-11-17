package org.zxc123zxc.sticker_pack_builder.bot

import java.util.UUID

import com.bot4s.telegram.api._
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.clients.ScalajHttpClient
import com.bot4s.telegram.methods.{AddStickerToSet, CreateNewStickerSet, GetFile, SendMessage}
import com.bot4s.telegram.models._
import org.zxc123zxc.sticker_pack_builder.webp_transformer.WebpTransformer
import scalaj.http.Http

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration


class StickerBuilderBot(private val _token: String, private val _libWebpPath: String)
  extends TelegramBot
  with Polling
  with Commands {
  import com.bot4s.telegram.marshalling.CirceEncoders._
  import com.bot4s.telegram.marshalling.CirceDecoders._
  import BotMessages._

  private val _botName = "StickerSetBuilderBot"
  private var _state = Map[Long, StickerSetBuilderState]()
  private val _converter = new WebpTransformer(_libWebpPath)

  override val client = new ScalajHttpClient(_token)


  onSticker((msg, sticker) => {
    val chatId = msg.chat.id
    val userId = msg.from.get.id
    val state = _state.applyOrElse[Long, StickerSetBuilderState](chatId, _ => Idle())

    state match {
      case StartedCreation() => send(chatId, chooseNameFirstly)
      case TitleChosen(setTitle) =>
        _state += (chatId -> Loading(state))
        send(chatId, creatingSet(setTitle))

        getFile(sticker.fileId)
          .flatMap(file => getFileBytes(_token, file.filePath.get))
          .flatMap(bytes => Future {_converter.convertToPng(bytes)})
          .flatMap(bytes => createStickerSet(userId, setTitle, bytes))
          .map({case (_, setName) =>
            _state += (chatId -> StickerAdd(setName, StickerSetModification()))
            send(chatId, s"${hereYourLink(formatLink(setName))}" +
              "\nContinue adding stickers to set " +
              "\nAnd type /done when you will be finished")
          })

      case StickerAdd(setName, modification) =>
        val additions = modification.addFileIds ::: List(sticker.fileId)
        val modifications = modification.copy(addFileIds = additions)

        send(chatId, "Added. Already done? Type /done")
        _state += (chatId -> StickerAdd(setName, modifications))

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

  onCommand("/done")(msg => {
    val chatId = msg.chat.id
    val userId = msg.from.get.id
    val state = _state.applyOrElse[Long, StickerSetBuilderState](chatId, _ => Idle())

    state match {
      case StickerAdd(setName, m) =>
        _state += (chatId -> Loading(state))
        send(chatId, processingImages)

        val futures = m.addFileIds.map(fileId => {
          getFile(fileId)
            .flatMap(file => getFileBytes(_token, file.filePath.get))
            .flatMap(bytes => Future {_converter.convertToPng(bytes)})
        })

        val complete = Future.sequence(futures).flatMap(bytesList => {
          send(chatId, addingToSet)

          val futures = bytesList.map(bytes => addStickerToSet(userId, setName, bytes))
          Future.sequence(futures)
        })

        Await.result(complete, Duration.Inf)

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

  private def send(chatId: Long, msg: String): Unit = {
    val sendMsgReq = SendMessage(chatId, msg)
    client.sendRequest[Message, SendMessage](sendMsgReq)
  }

  private def createStickerSet(userId: Int, title: String, pngBytes: Array[Byte], fileName: String = "", emojis:String = "\uD83D\uDC31"): Future[(Boolean, String)] = {
    val id = UUID.randomUUID().toString.replace('-', '_')
    val name = s"${id}_by_${_botName}"
    val req = CreateNewStickerSet(
      userId,
      name,
      title,
      InputFile(fileName, pngBytes),
      emojis)

    val res = client.sendRequest[Boolean, CreateNewStickerSet](req)

    res.map(r => (r, name))
  }

  private def addStickerToSet(userId: Int, name: String, pngBytes: Array[Byte], fileName: String = "", emojis:String = "\uD83D\uDC31"): Future[Boolean] = {
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
