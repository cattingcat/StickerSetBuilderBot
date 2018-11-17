package org.zxc123zxc.sticker_pack_builder.bot

object BotMessages {
  def welcome: String = "Welcome to StickerSetBuilder! \nType /create to build your personal sticker set from existing ones"
  def typeCreate: String = "Start creating new sticker set with /create command"
  def chooseName: String = "Okay, I'm ready to make sticker set! \nType new sticker set name (length < 64)"
  def readyToReceive: String = "Good job! Now I'm ready to receive first sticker in the set. \nRemember first sticker will be shown as sticker set logo"
  def chooseNameFirstly: String = "Sorry, choose the name firstly ..."
  def creatingSet(title: String): String = s"Creating sticker set $title"
  def creatingSet: String = "Creating sticker set ..."
  def hereYourLink(link: String): String = s"Okay, Here is your link $link"
  def processingImages: String = "Processing images ..."
  def addingToSet: String = "Images processed. Adding images to set ..."
  def loading: String = "Something processing, wait please ..."
  def invalidTitle: String = "Invalid sticker set title"
}
