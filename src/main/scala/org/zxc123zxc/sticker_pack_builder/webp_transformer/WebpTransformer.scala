package org.zxc123zxc.sticker_pack_builder.webp_transformer

import java.io._
import java.nio.file.{Files, Paths, StandardCopyOption}
import java.util.UUID

import scala.sys.process._


trait WebpTransformerBase {
  private val _prefix = "webp_tr"

  def convertToPng(src: String, dest: String): Unit

  def convertToPng(src: InputStream): (InputStream, Long) = {
    val uuid = UUID.randomUUID
    val tempDirPath = Files.createTempDirectory(_prefix)
    val srcPath = Files.createTempFile(tempDirPath, _prefix, uuid.toString)

    Files.copy(src, srcPath, StandardCopyOption.REPLACE_EXISTING)

    val destPath = Paths.get(tempDirPath.toString, s"$uuid-out")

    convertToPng(srcPath.toString, destPath.toString)

    val outFile = destPath.toFile

    (new FileInputStream(outFile), outFile.length)
  }

  def convertToPng(src: Array[Byte]): Array[Byte] = {
    val srcStream = new ByteArrayInputStream(src)

    val (destStream, len) = convertToPng(srcStream)

    srcStream.close()

    val array = new Array[Byte](len.asInstanceOf[Int])
    destStream.read(array)
    destStream.close()

    array
  }
}

/** Transforms WebP to PNG */
class WebpTransformer(val libWebpPath: String) extends WebpTransformerBase {
  override def convertToPng(src: String, dest: String): Unit = {
    s"$libWebpPath/bin/dwebp $src -o $dest" !
  }
}
