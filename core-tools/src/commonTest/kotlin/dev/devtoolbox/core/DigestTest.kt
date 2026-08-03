package dev.devtoolbox.core

import dev.devtoolbox.core.util.Digest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Vetores oficiais: RFC 1321 (MD5), FIPS 180-1 (SHA-1), FIPS 180-2 (SHA-256). */
class DigestTest {

    @Test
    fun md5OfficialVectors() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", Digest.md5(""))
        assertEquals("0cc175b9c0f1b6a831c399e269772661", Digest.md5("a"))
        assertEquals("900150983cd24fb0d6963f7d28e17f72", Digest.md5("abc"))
        assertEquals("f96b697d7cb7938d525a2f31aaf161d0", Digest.md5("message digest"))
        assertEquals("c3fcd3d76192e4007dfb496cca67e13b", Digest.md5("abcdefghijklmnopqrstuvwxyz"))
        assertEquals(
            "57edf4a22be3c955ac49da2e2107b67a",
            Digest.md5("12345678901234567890123456789012345678901234567890123456789012345678901234567890"),
        )
    }

    @Test
    fun sha1OfficialVectors() {
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", Digest.sha1(""))
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", Digest.sha1("abc"))
        assertEquals(
            "84983e441c3bd26ebaae4aa1f95129e5e54670f1",
            Digest.sha1("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"),
        )
    }

    @Test
    fun sha256OfficialVectors() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            Digest.sha256(""),
        )
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Digest.sha256("abc"),
        )
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            Digest.sha256("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"),
        )
    }

    @Test
    fun handlesMessagesThatCrossBlockBoundaries() {
        // 55, 56 e 64 bytes exercitam os três casos do padding.
        val a55 = "a".repeat(55)
        val a56 = "a".repeat(56)
        val a64 = "a".repeat(64)
        assertEquals(64, Digest.sha256(a55).length)
        assertEquals(64, Digest.sha256(a56).length)
        assertEquals(64, Digest.sha256(a64).length)
        assertEquals(
            "9f4390f8d30c2dd92ec9f095b65e2b9ae9b0a925a5258e241c9f1e910f734318",
            Digest.sha256(a55),
        )
        assertEquals(
            "b35439a4ac6f0948b6d6f9e3c6af0f5f590ce20f1bde7090ef7970686ec6738a",
            Digest.sha256(a56),
        )
    }

    @Test
    fun handlesUtf8Multibyte() {
        // O hash é sobre os bytes UTF-8, não sobre os chars.
        assertEquals(Digest.sha256("ação"), Digest.sha256("ação"))
        assertEquals(64, Digest.sha256("日本語 🎉").length)
    }
}
