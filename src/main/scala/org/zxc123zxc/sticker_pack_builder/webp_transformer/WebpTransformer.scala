package org.zxc123zxc.sticker_pack_builder.webp_transformer

import java.io._
import java.nio.file.{Files, Paths, StandardCopyOption}
import java.util.UUID

import sys.process._

object WebpTransformer {
  val home = System.getProperty("user.home")
  val libWebpPath = s"${home}/Desktop/libwebp-1.0.1-rc2-mac-10.13"

  def transformToPng(src: String, dest: String): Unit = {
    val path = Paths.get(src)
    val res = s"${libWebpPath}/bin/dwebp ${src} -o ${dest}" !

    println(s"dwebp result: $res")
  }

  def transformToPng(srcWebp: InputStream): (InputStream, Long) = {
    val uuid = UUID.randomUUID()
    val tempDirPath = Files.createTempDirectory("webp_transformer")
    val srcFilePath = Files.createTempFile(tempDirPath, "webp_transformer", uuid.toString)

    Files.copy(srcWebp, srcFilePath, StandardCopyOption.REPLACE_EXISTING)

    val outFilePath = Paths.get(tempDirPath.toString, s"${uuid}-out")

    transformToPng(srcFilePath.toString, outFilePath.toString)

    val outFile = outFilePath.toFile

    (new FileInputStream(outFile), outFile.length)
  }

  def transformToPng(src: Array[Byte]): Array[Byte] = {
    val webpStream = new ByteArrayInputStream(src)

    val (pngStream, len) = WebpTransformer.transformToPng(webpStream)

    webpStream.close()

    val array = new Array[Byte](len.asInstanceOf[Int])
    pngStream.read(array)
    pngStream.close()

    array
  }
}
