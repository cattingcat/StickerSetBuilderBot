package org.zxc123zxc.stickerPackBuilder.bot

trait PotentialSticker
case class StickerFile(fileId: String) extends PotentialSticker
case class PngFile(fileId: String) extends PotentialSticker

sealed abstract class StickerSetBuilderState
final case class Idle() extends StickerSetBuilderState
final case class StartedCreation() extends StickerSetBuilderState
final case class StartedEditing() extends StickerSetBuilderState
final case class TitleChosen(title: String) extends StickerSetBuilderState
final case class StickerAdd(name: String, modification: StickerSetModification) extends StickerSetBuilderState
final case class StickerRemove(name: String, modification: StickerSetModification) extends StickerSetBuilderState
final case class Loading(originalState: StickerSetBuilderState) extends StickerSetBuilderState

case class StickerSetModification(addFileIds: List[PotentialSticker] = Nil, removeFileIds: List[String] = Nil)