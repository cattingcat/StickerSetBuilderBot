package org.zxc123zxc.sticker_pack_builder.bot

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
        send(chatId, creatingSet(setTitle))

        getFile(sticker.fileId)
          .flatMap(file => getFileBytes(_token, file.filePath.get))
          .flatMap(bytes => Future {_converter.convertToPng(bytes)})
          .flatMap(bytes => createStickerSet(userId, setTitle, bytes))
          .map({case (_, setName) =>
            _state += (chatId -> StickerAdd(setName, StickerSetModification()))
            send(chatId, s"Your sticker set here: ${formatLink(setName)}" +
              "\nContinue adding stickers to set " +
              "\nAnd type /done when you will be finished")
          })

      case StickerAdd(setName, modification) =>
        val additions = modification.addFileIds ::: List(sticker.fileId)
        val modifications = modification.copy(addFileIds = additions)

        send(chatId, "Added. Already done? Type /done")
        _state += (chatId -> StickerAdd(setName, modifications))

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

    _state(chatId) match {
      case StickerAdd(setName, m) =>
        send(chatId, processingImages)

        val futures = m.addFileIds.map(fileId => {
          getFile(fileId)
            .flatMap(file => getFileBytes(_token, file.filePath.get))
            .flatMap(bytes => Future {_converter.convertToPng(bytes)})
        })

        val complete = Future.sequence(futures).flatMap(bytesList => {
          send(chatId, s"Images processed. Adding...")

          val futures = bytesList.map(bytes => addStickerToSet(userId, setName, bytes))
          Future.sequence(futures)
        })

        Await.result(complete, Duration.Inf)

        send(chatId, hereYourLink(formatLink(setName)))
        _state -= chatId

      case StickerRemove(setName, _) =>
        send(chatId, hereYourLink(formatLink(setName)))
        _state -= chatId

      case _ => send(chatId, typeCreate)
    }
  })

  onMessage(msg => {
    val chatId = msg.chat.id
    _state(chatId) match {
      case StartedCreation() =>
        msg.text match {
          case Some(text) if !text.contains('/') && text.length < (64 - 4 - _botName.length) =>
            _state += (chatId -> TitleChosen(text))
            send(chatId, readyToReceive)
        }
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

  private def createStickerSet(userId: Int, setTitle: String, pngBytes: Array[Byte], fileName: String = "", emojis:String = "\uD83D\uDC31"): Future[(Boolean, String)] = {
    val setName = s"${setTitle}_by_${_botName}"
    val req = CreateNewStickerSet(
      userId,
      setName,
      setTitle,
      InputFile(fileName, pngBytes),
      emojis)

    val resp = client.sendRequest[Boolean, CreateNewStickerSet](req)

    resp.map(res => (res, setName))
  }

  private def addStickerToSet(userId: Int, setName: String, pngBytes: Array[Byte], fileName: String = "", emojis:String = "\uD83D\uDC31"): Future[Boolean] = {
    val req = AddStickerToSet(
      userId,
      setName,
      InputFile(fileName, pngBytes),
      emojis)

    client.sendRequest[Boolean, AddStickerToSet](req)
  }

  private def getFile(fileId: String): Future[File] = {
    val fileReq = GetFile(fileId)
    val fileResp = client.sendRequest[File, GetFile](fileReq)
    fileResp
  }

  private def getFileBytes(token: String, filePath: String): Future[Array[Byte]] = Future {
    val uri = s"https://api.telegram.org/file/bot$token/$filePath"
    val resp = Http(uri).asBytes
    resp.body
  }

  private def formatLink(setName: String): String = s"t.me/addstickers/$setName"
}
