package dev.devtoolbox.core.util

/**
 * Leitura de imagens para data URI: tipo por *magic bytes*, dimensões pelo cabeçalho e
 * codificação em Base64.
 *
 * Tudo em `commonMain` e sem decodificar o pixel data — só os primeiros bytes de cada formato.
 * Além de manter o core multiplataforma, isso significa que abrir um PNG de 5 MB custa a
 * leitura do cabeçalho, não a rasterização da imagem inteira.
 */

/** Formato reconhecido, com o MIME que vai para a data URI. */
enum class ImageFormat(val mime: String, val label: String, val extensions: List<String>) {
    Png("image/png", "PNG", listOf("png")),
    Jpeg("image/jpeg", "JPEG", listOf("jpg", "jpeg", "jpe")),
    Gif("image/gif", "GIF", listOf("gif")),
    Webp("image/webp", "WebP", listOf("webp")),
    Bmp("image/bmp", "BMP", listOf("bmp")),
    Ico("image/x-icon", "ICO", listOf("ico")),
    Svg("image/svg+xml", "SVG", listOf("svg"));

    companion object {
        /** Extensões que o seletor de arquivos oferece. */
        val allExtensions: List<String> get() = entries.flatMap { it.extensions }
    }
}

/**
 * Imagem já codificada, pronta para a UI. [base64] não inclui o prefixo `data:`.
 *
 * [bytes] é o arquivo original, guardado para o preview: decodificar o Base64 de volta na hora
 * de desenhar custaria uma passada por megabytes de string a cada troca de ferramenta.
 */
class EncodedImage(
    val fileName: String,
    val format: ImageFormat,
    val width: Int?,
    val height: Int?,
    val bytes: ByteArray,
    val base64: String,
) {
    val originalBytes: Int get() = bytes.size

    val mime: String get() = format.mime

    val dataUri: String get() = "data:$mime;base64,$base64"

    /** O Base64 é ASCII, então o comprimento em caracteres **é** o tamanho em bytes. */
    val base64Bytes: Int get() = base64.length

    /** Nome sem extensão — vira o `alt` do snippet HTML. */
    val baseName: String get() = fileName.substringBeforeLast('.', fileName)

    val dimensions: String?
        get() = if (width != null && height != null) "$width × $height px" else null

    /**
     * Igualdade por identidade do conteúdo: comparar megabytes de `bytes` a cada emissão de
     * estado seria caro e não diria nada a mais — duas seleções só são "a mesma" quando são
     * o mesmo carregamento.
     */
    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = fileName.hashCode() * 31 + bytes.size

    override fun toString(): String =
        "EncodedImage($fileName, ${format.label}, ${width}x$height, ${bytes.size} B)"
}

sealed interface ImageEncodeResult {
    data class Ok(val image: EncodedImage) : ImageEncodeResult
    data class Error(val message: String) : ImageEncodeResult
}

object ImageEncoder {

    /** Teto de 5 MB no arquivo **original**. */
    const val MAX_BYTES: Int = 5 * 1024 * 1024

    fun encode(fileName: String, bytes: ByteArray): ImageEncodeResult {
        if (bytes.isEmpty()) return ImageEncodeResult.Error("O arquivo está vazio.")

        if (bytes.size > MAX_BYTES) {
            return ImageEncodeResult.Error(
                "A imagem tem ${formatBytes(bytes.size)} — o limite é ${formatBytes(MAX_BYTES)}.",
            )
        }

        val format = detectFormat(fileName, bytes)
            ?: return ImageEncodeResult.Error(
                "Formato não suportado. Use PNG, JPG, GIF, WebP, BMP, ICO ou SVG.",
            )

        val size = readDimensions(format, bytes)
        return ImageEncodeResult.Ok(
            EncodedImage(
                fileName = fileName,
                format = format,
                width = size?.first,
                height = size?.second,
                bytes = bytes,
                base64 = Base64.encodeBytes(bytes),
            ),
        )
    }

    /**
     * Tipo pelos *magic bytes*; SVG é a exceção — é texto, então cai para a extensão e para
     * uma farejada no começo do conteúdo.
     */
    fun detectFormat(fileName: String, bytes: ByteArray): ImageFormat? {
        when {
            bytes.startsWith(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> return ImageFormat.Png
            bytes.startsWith(0xFF, 0xD8, 0xFF) -> return ImageFormat.Jpeg
            bytes.startsWithAscii("GIF87a") || bytes.startsWithAscii("GIF89a") -> return ImageFormat.Gif
            bytes.startsWithAscii("RIFF") && bytes.asciiAt(8, 4) == "WEBP" -> return ImageFormat.Webp
            bytes.startsWithAscii("BM") -> return ImageFormat.Bmp
            bytes.startsWith(0x00, 0x00, 0x01, 0x00) -> return ImageFormat.Ico
        }

        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (extension == "svg" && looksLikeSvg(bytes)) return ImageFormat.Svg
        return null
    }

    /** `null` quando o cabeçalho não traz o tamanho (SVG sem `width`/`viewBox`, por exemplo). */
    fun readDimensions(format: ImageFormat, bytes: ByteArray): Pair<Int, Int>? = when (format) {
        ImageFormat.Png -> pngSize(bytes)
        ImageFormat.Jpeg -> jpegSize(bytes)
        ImageFormat.Gif -> gifSize(bytes)
        ImageFormat.Webp -> webpSize(bytes)
        ImageFormat.Bmp -> bmpSize(bytes)
        ImageFormat.Ico -> icoSize(bytes)
        ImageFormat.Svg -> svgSize(bytes)
    }

    // IHDR é sempre o primeiro chunk: largura e altura em big-endian nos offsets 16 e 20.
    private fun pngSize(b: ByteArray): Pair<Int, Int>? =
        if (b.size < 24) null else b.be32(16) to b.be32(20)

    /**
     * JPEG: percorre os segmentos até um SOF (0xC0–0xCF, menos os marcadores que não são
     * "start of frame"), onde altura e largura vivem nos offsets +5 e +7.
     */
    private fun jpegSize(b: ByteArray): Pair<Int, Int>? {
        var i = 2
        while (i + 9 < b.size) {
            if (b.u8(i) != 0xFF) {
                i++
                continue
            }
            val marker = b.u8(i + 1)
            val isStartOfFrame = marker in 0xC0..0xCF &&
                marker != 0xC4 && marker != 0xC8 && marker != 0xCC
            if (isStartOfFrame) {
                val height = b.be16(i + 5)
                val width = b.be16(i + 7)
                return if (width > 0 && height > 0) width to height else null
            }
            // Marcadores sem payload (RSTn, SOI, EOI, TEM) não trazem comprimento.
            if (marker == 0xD8 || marker == 0xD9 || marker == 0x01 || marker in 0xD0..0xD7) {
                i += 2
            } else {
                i += 2 + b.be16(i + 2)
            }
        }
        return null
    }

    private fun gifSize(b: ByteArray): Pair<Int, Int>? =
        if (b.size < 10) null else b.le16(6) to b.le16(8)

    private fun webpSize(b: ByteArray): Pair<Int, Int>? {
        if (b.size < 30) return null
        return when (b.asciiAt(12, 4)) {
            // Lossy: 14 bits de largura e altura logo após o sync code de 3 bytes.
            "VP8 " -> (b.le16(26) and 0x3FFF) to (b.le16(28) and 0x3FFF)
            // Lossless: largura-1 e altura-1 em 14 bits cada, empacotados a partir do offset 21.
            "VP8L" -> {
                val bits = b.le32(21)
                ((bits and 0x3FFF) + 1) to (((bits ushr 14) and 0x3FFF) + 1)
            }
            // Estendido: canvas em 24 bits little-endian, também guardado como valor-1.
            "VP8X" -> (b.le24(24) + 1) to (b.le24(27) + 1)
            else -> null
        }
    }

    // BMP guarda largura e altura como int32 com sinal; altura negativa = linhas de cima
    // para baixo, e o que interessa aqui é a magnitude.
    private fun bmpSize(b: ByteArray): Pair<Int, Int>? =
        if (b.size < 26) null else b.le32(18) to kotlin.math.abs(b.le32(22))

    // No diretório do ICO, 0 significa 256 — o campo tem um byte só.
    private fun icoSize(b: ByteArray): Pair<Int, Int>? {
        if (b.size < 8) return null
        val width = b.u8(6).let { if (it == 0) 256 else it }
        val height = b.u8(7).let { if (it == 0) 256 else it }
        return width to height
    }

    /** SVG: `width`/`height` quando são absolutos, senão as duas últimas casas do `viewBox`. */
    private fun svgSize(b: ByteArray): Pair<Int, Int>? {
        val head = b.decodeToString(0, minOf(b.size, 4096))
        val width = svgLength(head, "width")
        val height = svgLength(head, "height")
        if (width != null && height != null) return width to height

        val viewBox = Regex("""viewBox\s*=\s*["']([^"']+)["']""").find(head)?.groupValues?.get(1)
        val parts = viewBox?.trim()?.split(Regex("[\\s,]+")) ?: return null
        if (parts.size != 4) return null
        val w = parts[2].toDoubleOrNull() ?: return null
        val h = parts[3].toDoubleOrNull() ?: return null
        return if (w > 0 && h > 0) w.toInt() to h.toInt() else null
    }

    /** Ignora medidas relativas (`100%`, `10em`): elas não dizem o tamanho em pixels. */
    private fun svgLength(head: String, attribute: String): Int? {
        val raw = Regex("""\b$attribute\s*=\s*["']([^"']+)["']""").find(head)?.groupValues?.get(1)
            ?: return null
        val value = raw.trim().removeSuffix("px").trim()
        return value.toDoubleOrNull()?.takeIf { it > 0 }?.toInt()
    }

    private fun looksLikeSvg(bytes: ByteArray): Boolean {
        val head = bytes.decodeToString(0, minOf(bytes.size, 1024))
        return head.contains("<svg", ignoreCase = true) || head.trimStart().startsWith("<?xml")
    }

    /** Snippets prontos para colar, com a data URI inteira. */
    fun htmlSnippet(image: EncodedImage): String =
        """<img src="${image.dataUri}" alt="${image.baseName}">"""

    fun cssSnippet(image: EncodedImage): String =
        """background-image: url("${image.dataUri}");"""

    /**
     * Tamanho legível em pt-BR (vírgula decimal), uma casa a partir de KB.
     *
     * Usa KB/MB de 1024 — é o que os sistemas de arquivos mostram, e o número precisa bater
     * com o que o usuário vê no gerenciador de arquivos dele.
     */
    fun formatBytes(bytes: Int): String {
        if (bytes < 1024) return "$bytes B"
        val units = listOf("KB", "MB")
        var value = bytes / 1024.0
        var unit = 0
        while (value >= 1024 && unit < units.lastIndex) {
            value /= 1024.0
            unit++
        }
        // Uma casa decimal via inteiro: truncar em `Double` erra por representação
        // (18,4004 vira 18,3 quando se subtrai a parte inteira).
        val tenths = kotlin.math.round(value * 10).toLong()
        return "${tenths / 10},${tenths % 10} ${units[unit]}"
    }

    /** Acréscimo do Base64 sobre o original, arredondado — sempre perto de 33%. */
    fun growthPercent(originalBytes: Int, base64Bytes: Int): Int {
        if (originalBytes <= 0) return 0
        return ((base64Bytes - originalBytes) * 100.0 / originalBytes).toInt()
    }
}

private fun ByteArray.u8(index: Int): Int = this[index].toInt() and 0xFF

private fun ByteArray.be16(i: Int): Int = (u8(i) shl 8) or u8(i + 1)

private fun ByteArray.be32(i: Int): Int =
    (u8(i) shl 24) or (u8(i + 1) shl 16) or (u8(i + 2) shl 8) or u8(i + 3)

private fun ByteArray.le16(i: Int): Int = u8(i) or (u8(i + 1) shl 8)

private fun ByteArray.le24(i: Int): Int = u8(i) or (u8(i + 1) shl 8) or (u8(i + 2) shl 16)

private fun ByteArray.le32(i: Int): Int =
    u8(i) or (u8(i + 1) shl 8) or (u8(i + 2) shl 16) or (u8(i + 3) shl 24)

private fun ByteArray.startsWith(vararg prefix: Int): Boolean {
    if (size < prefix.size) return false
    return prefix.withIndex().all { (i, value) -> u8(i) == value }
}

private fun ByteArray.startsWithAscii(prefix: String): Boolean =
    size >= prefix.length && prefix.indices.all { u8(it) == prefix[it].code }

private fun ByteArray.asciiAt(offset: Int, length: Int): String? {
    if (size < offset + length) return null
    return buildString { for (i in offset until offset + length) append(u8(i).toChar()) }
}
