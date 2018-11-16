package org.zxc123zxc.sticker_pack_builder.bot

abstract class StickerSetBuilderState
final case class StartedCreation() extends StickerSetBuilderState
final case class StartedEditing() extends StickerSetBuilderState
final case class TitleChosen(setTitle: String) extends StickerSetBuilderState
final case class StickerAdd(setName: String, modification: StickerSetModification) extends StickerSetBuilderState
final case class StickerRemove(setName: String, modification: StickerSetModification) extends StickerSetBuilderState

case class StickerSetModification(addFileIds: List[String] = Nil, removeFileIds: List[String] = Nil)