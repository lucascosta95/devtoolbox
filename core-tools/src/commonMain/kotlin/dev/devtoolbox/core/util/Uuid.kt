package dev.devtoolbox.core.util

import kotlin.random.Random

object Uuid {

    fun v4(random: Random): String {
        val bytes = ByteArray(16) { random.nextInt(256).toByte() }
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()
        val hex = bytes.toHex()
        return listOf(
            hex.substring(0, 8),
            hex.substring(8, 12),
            hex.substring(12, 16),
            hex.substring(16, 20),
            hex.substring(20, 32),
        ).joinToString("-")
    }

    fun v4Batch(count: Int, seed: Int): List<String> {
        val random = Random(seed)
        return (0 until count).map { v4(random) }
    }
}
