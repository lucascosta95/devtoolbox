package dev.devtoolbox.app

import org.jetbrains.skia.EncodedImageFormat
import java.io.ByteArrayOutputStream
import java.io.File

fun main(args: Array<String>) {
    val outDir = File(args.getOrElse(0) { "src/jvmMain/resources/icons" }).apply { mkdirs() }

    val png512 = renderPng(512)
    File(outDir, "devtoolbox.png").writeBytes(png512)

    val icoSizes = listOf(16, 32, 48, 64, 128, 256)
    File(outDir, "devtoolbox.ico").writeBytes(buildIco(icoSizes.map { it to renderPng(it) }))

    val icnsEntries = listOf(
        "ic11" to 32, "ic12" to 64, "ic07" to 128,
        "ic13" to 256, "ic08" to 256, "ic14" to 512, "ic09" to 512,
    )
    File(outDir, "devtoolbox.icns").writeBytes(buildIcns(icnsEntries.map { (t, s) -> t to renderPng(s) }))

    println("ícones escritos em ${outDir.absolutePath}: devtoolbox.png, .ico, .icns")
}

private fun renderPng(size: Int): ByteArray =
    renderAppIcon(size).encodeToData(EncodedImageFormat.PNG)?.bytes
        ?: error("falha ao codificar o PNG de ${size}px")

private fun buildIco(images: List<Pair<Int, ByteArray>>): ByteArray {
    val out = ByteArrayOutputStream()

    fun le16(v: Int) { out.write(v and 0xFF); out.write((v shr 8) and 0xFF) }
    fun le32(v: Int) {
        out.write(v and 0xFF); out.write((v shr 8) and 0xFF)
        out.write((v shr 16) and 0xFF); out.write((v shr 24) and 0xFF)
    }

    le16(0)
    le16(1)
    le16(images.size)

    var offset = 6 + images.size * 16
    for ((size, png) in images) {
        out.write(if (size >= 256) 0 else size)
        out.write(if (size >= 256) 0 else size)
        out.write(0)
        out.write(0)
        le16(1)
        le16(32)
        le32(png.size)
        le32(offset)
        offset += png.size
    }
    for ((_, png) in images) out.write(png)
    return out.toByteArray()
}

private fun buildIcns(entries: List<Pair<String, ByteArray>>): ByteArray {
    val body = ByteArrayOutputStream()

    fun be32(stream: ByteArrayOutputStream, v: Int) {
        stream.write((v shr 24) and 0xFF); stream.write((v shr 16) and 0xFF)
        stream.write((v shr 8) and 0xFF); stream.write(v and 0xFF)
    }

    for ((type, png) in entries) {
        body.write(type.encodeToByteArray())
        be32(body, png.size + 8)
        body.write(png)
    }

    val out = ByteArrayOutputStream()
    out.write("icns".encodeToByteArray())
    be32(out, body.size() + 8)
    out.write(body.toByteArray())
    return out.toByteArray()
}
