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

class StickerBuilderBot(val token: String)
  extends TelegramBot
  with Polling
  with Commands {
  self =>
  import com.bot4s.telegram.marshalling.CirceEncoders._
  import com.bot4s.telegram.marshalling.CirceDecoders._

  private val botName = "StickerSetBuilderBot"
  private var state = Map[Long, StickerSetBuilderState]()
  val client = new ScalajHttpClient(token)

  onSticker((msg, sticker) => {
    val chatId = msg.chat.id
    val userId = msg.from.get.id

    state(chatId) match {
      case StartedCreation() => sendMsg(chatId, "Sorry, choose name firstly...")
      case TitleChosen(setTitle) =>
        sendMsg(chatId, s"Creating stricker set $setTitle")

        getFile(sticker.fileId)
          .flatMap(file => getFileBytes(token, file.filePath.get))
          .flatMap(bytes => Future {WebpTransformer.transformToPng(bytes)})
          .flatMap(bytes => createStickerSet(userId, setTitle, bytes))
          .map({case (_, setName) =>
            state += (chatId -> StickerAdd(setName, StickerSetModification()))
            sendMsg(chatId, s"Your sticker set here: ${formatLink(setName)}" +
              "\nContinue adding stickers to set " +
              "\nAnd type /done when you will be finished")
          })
      case StickerAdd(setName, modification) =>
        val additions = modification.addFileIds ::: List(sticker.fileId)
        val modifications = modification.copy(addFileIds = additions)

        state += (chatId -> StickerAdd(setName, modifications))

        sendMsg(chatId, "Added")
    }
  })



  onCommand("/start")(msg => {
    sendMsg(msg.chat.id, "Welcome to StickerSetBuilder! \nType /create to build your personal sticker set from existing ones")
  })

  onCommand("/create")(msg => {
    sendMsg(msg.chat.id, "Okay, I'm ready to make sticker set! \nType new sticker set name")
    state += (msg.chat.id -> StartedCreation())
  })

  onCommand("/done")(msg => {
    val chatId = msg.chat.id
    val userId = msg.from.get.id

    state(chatId) match {
      case StickerAdd(setName, m) =>
        sendMsg(chatId, s"Creating sticker set...")

        val futures = m.addFileIds.map(fileId => {
          getFile(fileId)
            .flatMap(file => getFileBytes(token, file.filePath.get))
            .flatMap(bytes => Future {WebpTransformer.transformToPng(bytes)})
        })

        val complete = Future.sequence(futures).flatMap(bytesList => {
          sendMsg(chatId, s"Images processed. Adding...")

          val futures = bytesList.map(bytes => addStickerToSet(userId, setName, bytes))
          Future.sequence(futures)
        })

        Await.result(complete, Duration.Inf)

        sendMsg(chatId, s"Okay, Here is your link ${formatLink(setName)}")

        state -= chatId
      case StickerRemove(setName, _) =>
        sendMsg(msg.chat.id, s"Okay, Here is your link ${formatLink(setName)}")
        state -= chatId
    }
  })

  onMessage(msg => {
    val chatId = msg.chat.id
    state(chatId) match {
      case StartedCreation() =>
        msg.text match {
          case Some(text) if !text.contains('/') && text.length < (64 - 4 - botName.length) =>
            state += (chatId -> TitleChosen(text))
            sendMsg(chatId, "Good job! Now I'm ready to receive first sticker in the set. \nRemember first sticker will be shown as sticker set logo")
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

  private def sendMsg(chatId: Long, msg: String): Unit = {
    val sendMsgReq = SendMessage(chatId, msg)
    client.sendRequest[Message, SendMessage](sendMsgReq)
  }

  private def createStickerSet(userId: Int, setTitle: String, pngBytes: Array[Byte]): Future[(Boolean, String)] = {
    val setName = s"${setTitle}_by_$botName"
    val req = CreateNewStickerSet(
      userId,
      setName,
      setTitle,
      InputFile("test", pngBytes),
      "\uD83D\uDC31")

    val resp = client.sendRequest[Boolean, CreateNewStickerSet](req)

    resp.map(res => (res, setName))
  }

  private def addStickerToSet(userId: Int, setName: String, pngBytes: Array[Byte]): Future[Boolean] = {
    val req = AddStickerToSet(
      userId,
      setName,
      InputFile("test", pngBytes),
      "\uD83D\uDC31")

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
