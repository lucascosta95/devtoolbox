package dev.devtoolbox.core

import dev.devtoolbox.core.tools.ImageBase64Tool
import dev.devtoolbox.core.util.Base64
import dev.devtoolbox.core.util.ImageEncodeResult
import dev.devtoolbox.core.util.ImageEncoder
import dev.devtoolbox.core.util.ImageFormat
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ImageDataTest {
    private fun png(width: Int, height: Int) = ByteArray(64).also { b ->
        byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A).copyInto(b)
        "IHDR".encodeToByteArray().copyInto(b, 12)
        b.putBe32(16, width)
        b.putBe32(20, height)
    }

    private fun jpeg(width: Int, height: Int) = ByteArray(32).also { b ->
        byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xC0.toByte(), 0x00, 0x11, 0x08)
            .copyInto(b)
        b.putBe16(7, height)
        b.putBe16(9, width)
    }

    private fun gif(width: Int, height: Int) = ByteArray(16).also { b ->
        "GIF89a".encodeToByteArray().copyInto(b)
        b.putLe16(6, width)
        b.putLe16(8, height)
    }

    private fun webpLossy(width: Int, height: Int) = ByteArray(40).also { b ->
        "RIFF".encodeToByteArray().copyInto(b)
        "WEBP".encodeToByteArray().copyInto(b, 8)
        "VP8 ".encodeToByteArray().copyInto(b, 12)
        b.putLe16(26, width)
        b.putLe16(28, height)
    }

    private fun bmp(width: Int, height: Int) = ByteArray(32).also { b ->
        "BM".encodeToByteArray().copyInto(b)
        b.putLe32(18, width)
        b.putLe32(22, height)
    }

    private fun ico(width: Int, height: Int) = ByteArray(16).also { b ->
        byteArrayOf(0x00, 0x00, 0x01, 0x00, 0x01, 0x00).copyInto(b)
        b[6] = width.toByte()
        b[7] = height.toByte()
    }

    private fun svg(attributes: String) =
        """<svg xmlns="http://www.w3.org/2000/svg" $attributes><rect/></svg>""".encodeToByteArray()

    @Test
    fun encodesAndDecodesBackToTheOriginalBytes() {
        val original = png(512, 512)
        val result = ImageEncoder.encode("logo.png", original)

        val image = assertIs<ImageEncodeResult.Ok>(result).image
        assertEquals(original.toList(), Base64.decodeToBytes(image.base64).toList())
    }

    @Test
    fun buildsADataUriWithTheDetectedMime() {
        val image = assertIs<ImageEncodeResult.Ok>(ImageEncoder.encode("logo.png", png(8, 8))).image

        assertTrue(image.dataUri.startsWith("data:image/png;base64,"))
        assertEquals(image.base64, image.dataUri.substringAfter("base64,"))
    }

    @Test
    fun detectsFormatsByMagicBytesRegardlessOfTheExtension() {
        assertEquals(ImageFormat.Png, ImageEncoder.detectFormat("a.txt", png(1, 1)))
        assertEquals(ImageFormat.Jpeg, ImageEncoder.detectFormat("a.png", jpeg(1, 1)))
        assertEquals(ImageFormat.Gif, ImageEncoder.detectFormat("a.bin", gif(1, 1)))
        assertEquals(ImageFormat.Webp, ImageEncoder.detectFormat("a.jpg", webpLossy(1, 1)))
        assertEquals(ImageFormat.Bmp, ImageEncoder.detectFormat("a.gif", bmp(1, 1)))
        assertEquals(ImageFormat.Ico, ImageEncoder.detectFormat("a.png", ico(16, 16)))
    }

    @Test
    fun svgIsDetectedByExtensionBecauseItIsText() {
        assertEquals(ImageFormat.Svg, ImageEncoder.detectFormat("icon.svg", svg("""width="24"""")))
        assertNull(ImageEncoder.detectFormat("icon.xml", svg("""width="24"""")))
    }

    @Test
    fun unknownContentIsRejected() {
        val result = ImageEncoder.encode("relatorio.pdf", "%PDF-1.7\n%…".encodeToByteArray())
        assertContains(assertIs<ImageEncodeResult.Error>(result).message, "Formato não suportado")
    }

    @Test
    fun readsDimensionsFromEachHeader() {
        fun size(name: String, bytes: ByteArray) =
            assertIs<ImageEncodeResult.Ok>(ImageEncoder.encode(name, bytes)).image
                .let { it.width to it.height }

        assertEquals(512 to 256, size("a.png", png(512, 256)))
        assertEquals(640 to 480, size("a.jpg", jpeg(640, 480)))
        assertEquals(120 to 90, size("a.gif", gif(120, 90)))
        assertEquals(300 to 200, size("a.webp", webpLossy(300, 200)))
        assertEquals(64 to 32, size("a.bmp", bmp(64, 32)))
        assertEquals(48 to 48, size("a.ico", ico(48, 48)))
    }

    @Test
    fun icoTreatsZeroAsTwoHundredAndFiftySix() {
        val image = assertIs<ImageEncodeResult.Ok>(ImageEncoder.encode("a.ico", ico(0, 0))).image
        assertEquals(256 to 256, image.width to image.height)
    }

    @Test
    fun bmpWithNegativeHeightReportsTheMagnitude() {
        val bytes = bmp(64, 32).also { it.putLe32(22, -32) }
        val image = assertIs<ImageEncodeResult.Ok>(ImageEncoder.encode("a.bmp", bytes)).image
        assertEquals(32, image.height)
    }

    @Test
    fun svgFallsBackToViewBoxAndIgnoresRelativeSizes() {
        fun size(attributes: String) =
            assertIs<ImageEncodeResult.Ok>(ImageEncoder.encode("i.svg", svg(attributes))).image

        assertEquals(24 to 24, size("""width="24" height="24"""").let { it.width to it.height })
        assertEquals(24 to 24, size("""width="24px" height="24px"""").let { it.width to it.height })
        val relative = size("""width="100%" height="100%" viewBox="0 0 256 128"""")
        assertEquals(256 to 128, relative.width to relative.height)
        assertNull(size("").width)
    }

    @Test
    fun base64SizeIsTheEncodedLengthAndGrowsAboutAThird() {
        val bytes = ByteArray(3_000) { it.toByte() }
        val image = assertIs<ImageEncodeResult.Ok>(ImageEncoder.encode("a.png", pngOf(bytes))).image

        val expected = (image.originalBytes + 2) / 3 * 4
        assertEquals(expected, image.base64Bytes)
        assertEquals(33, ImageEncoder.growthPercent(image.originalBytes, image.base64Bytes))
    }

    @Test
    fun rejectsFilesOverFiveMegabytes() {
        val tooBig = pngOf(ByteArray(ImageEncoder.MAX_BYTES + 1))
        val message = assertIs<ImageEncodeResult.Error>(ImageEncoder.encode("g.png", tooBig)).message

        assertContains(message, "o limite é")
        assertContains(message, "5,0 MB")
    }

    @Test
    fun acceptsAFileExactlyAtTheLimit() {
        val atLimit = ByteArray(ImageEncoder.MAX_BYTES).also { png(1, 1).copyInto(it) }
        assertIs<ImageEncodeResult.Ok>(ImageEncoder.encode("no-limite.png", atLimit))
    }

    @Test
    fun rejectsAnEmptyFile() {
        assertIs<ImageEncodeResult.Error>(ImageEncoder.encode("vazio.png", ByteArray(0)))
    }

    @Test
    fun formatsSizesInPtBr() {
        assertEquals("512 B", ImageEncoder.formatBytes(512))
        assertEquals("1,0 KB", ImageEncoder.formatBytes(1024))
        assertEquals("18,4 KB", ImageEncoder.formatBytes(18_842))
        assertEquals("5,0 MB", ImageEncoder.formatBytes(5 * 1024 * 1024))
    }

    @Test
    fun snippetsCarryTheWholeDataUri() {
        val image = assertIs<ImageEncodeResult.Ok>(ImageEncoder.encode("logo.png", png(8, 8))).image

        val html = ImageEncoder.htmlSnippet(image)
        assertEquals("""<img src="${image.dataUri}" alt="logo">""", html)
        assertEquals("""background-image: url("${image.dataUri}");""", ImageEncoder.cssSnippet(image))
    }

    @Test
    fun toolExposesRowsDataUriAndSnippets() {
        val bytes = png(512, 512) + ByteArray(9_000)
        val image = assertIs<ImageEncodeResult.Ok>(ImageEncoder.encode("logo.png", bytes)).image
        val output = ImageBase64Tool.run(ToolInput.Image(ImageSelection.Loaded(image)))
        val body = assertIs<ToolOutput.Success>(output).body as ToolBody.Image
        val details = body.details!!

        assertEquals(
            listOf("Arquivo", "Dimensões", "Tipo MIME", "Tamanho original", "Tamanho em Base64"),
            details.rows.map { it.label },
        )
        assertEquals("512 × 512 px", details.rows[1].value)
        assertEquals("image/png", details.rows[2].value)
        assertContains(details.rows[4].value, "+33%")
        assertEquals(listOf("HTML", "CSS"), details.snippets.map { it.label })
    }

    @Test
    fun toolReportsTheEmptyAndLoadingStates() {
        val empty = assertIs<ToolOutput.Success>(ImageBase64Tool.run(ToolInput.Image()))
        assertEquals(ToolBody.Image(), empty.body)

        val loading = ImageBase64Tool.run(ToolInput.Image(ImageSelection.Loading("a.png")))
        assertTrue((assertIs<ToolOutput.Success>(loading).body as ToolBody.Image).loading)
    }

    @Test
    fun toolSurfacesTheFailureMessageButKeepsTheDropZone() {
        val failed = ImageSelection.Failed("g.png", "A imagem tem 8,0 MB — o limite é 5,0 MB.")
        val output = assertIs<ToolOutput.Failure>(ImageBase64Tool.run(ToolInput.Image(failed)))

        assertEquals("A imagem tem 8,0 MB — o limite é 5,0 MB.", output.message)
        assertEquals(ToolBody.Image(), output.body)
    }

    @Test
    fun keepsTheOriginalBytesForThePreview() {
        val original = png(320, 200)
        val image = assertIs<ImageEncodeResult.Ok>(ImageEncoder.encode("logo.png", original)).image

        assertEquals(original.toList(), image.bytes.toList())
        assertEquals(original.size, image.originalBytes)
    }

    @Test
    fun twoLoadsOfTheSameFileAreDistinctSelections() {
        val bytes = png(8, 8)
        val first = assertIs<ImageEncodeResult.Ok>(ImageEncoder.encode("a.png", bytes)).image
        val second = assertIs<ImageEncodeResult.Ok>(ImageEncoder.encode("a.png", bytes)).image

        assertEquals(first, first)
        assertNotEquals(first, second)
    }

    @Test
    fun toolExposesTheImageAsThePreviewSource() {
        val image = assertIs<ImageEncodeResult.Ok>(ImageEncoder.encode("logo.png", png(64, 64))).image
        val output = ImageBase64Tool.run(ToolInput.Image(ImageSelection.Loaded(image)))
        val body = assertIs<ToolOutput.Success>(output).body as ToolBody.Image

        assertSame(image, body.source)
    }

    @Test
    fun removingTheImageClearsEverything() {
        val image = assertIs<ImageEncodeResult.Ok>(ImageEncoder.encode("logo.png", png(64, 64))).image
        val loaded = ImageBase64Tool.run(ToolInput.Image(ImageSelection.Loaded(image)))
        assertNotNull((assertIs<ToolOutput.Success>(loaded).body as ToolBody.Image).details)

        val cleared = ImageBase64Tool.run(ToolInput.Image(ImageSelection.Empty))
        val body = assertIs<ToolOutput.Success>(cleared).body as ToolBody.Image

        assertNull(body.details)
        assertNull(body.source)
        assertEquals(ToolBody.Image(), body)
    }

    @Test
    fun loadingAndFailureDoNotCarryAPreview() {
        val loading = ImageBase64Tool.run(ToolInput.Image(ImageSelection.Loading("a.png")))
        assertNull((assertIs<ToolOutput.Success>(loading).body as ToolBody.Image).source)

        val failed = ImageSelection.Failed("a.psd", "Formato não suportado.")
        val body = assertIs<ToolOutput.Failure>(ImageBase64Tool.run(ToolInput.Image(failed))).body
        assertNull((body as ToolBody.Image).source)
    }

    @Test
    fun toolIsRegisteredUnderEncoding() {
        val tool = ToolRegistry.byId("img64")
        assertEquals(Category.Encoding, tool?.category)
        assertEquals("image", tool?.icon)
        assertEquals("hash", ToolRegistry.all[ToolRegistry.all.indexOfFirst { it.id == "img64" } - 1].id)
    }

    private fun pngOf(payload: ByteArray) = png(1, 1) + payload
}

private fun ByteArray.putBe32(i: Int, value: Int) {
    this[i] = (value ushr 24).toByte()
    this[i + 1] = (value ushr 16).toByte()
    this[i + 2] = (value ushr 8).toByte()
    this[i + 3] = value.toByte()
}

private fun ByteArray.putBe16(i: Int, value: Int) {
    this[i] = (value ushr 8).toByte()
    this[i + 1] = value.toByte()
}

private fun ByteArray.putLe16(i: Int, value: Int) {
    this[i] = value.toByte()
    this[i + 1] = (value ushr 8).toByte()
}

private fun ByteArray.putLe32(i: Int, value: Int) {
    this[i] = value.toByte()
    this[i + 1] = (value ushr 8).toByte()
    this[i + 2] = (value ushr 16).toByte()
    this[i + 3] = (value ushr 24).toByte()
}
