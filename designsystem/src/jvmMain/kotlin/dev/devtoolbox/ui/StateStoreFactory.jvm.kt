package dev.devtoolbox.ui

import dev.devtoolbox.core.persistence.PersistedState
import dev.devtoolbox.core.persistence.StateCodec
import dev.devtoolbox.core.persistence.StateStore
import java.io.File

/**
 * Estado em `state.json`, no diretório de configuração de cada SO:
 * `$XDG_CONFIG_HOME/devtoolbox` (Linux), `~/Library/Application Support/DevToolbox` (macOS)
 * e `%APPDATA%\DevToolbox` (Windows).
 */
class FileStateStore(private val file: File) : StateStore {

    override fun load(): PersistedState? =
        runCatching { if (file.isFile) StateCodec.decode(file.readText()) else null }.getOrNull()

    override fun save(state: PersistedState) {
        runCatching {
            file.parentFile?.mkdirs()
            // Escrita atômica: um desligamento no meio não deixa um JSON pela metade.
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(StateCodec.encode(state))
            if (!temp.renameTo(file)) {
                file.writeText(temp.readText())
                temp.delete()
            }
        }
    }

    companion object {
        fun defaultFile(): File {
            val os = System.getProperty("os.name").orEmpty().lowercase()
            val home = System.getProperty("user.home").orEmpty()
            val dir = when {
                os.contains("mac") || os.contains("darwin") ->
                    File(home, "Library/Application Support/DevToolbox")
                os.contains("win") ->
                    File(System.getenv("APPDATA") ?: File(home, "AppData/Roaming").path, "DevToolbox")
                else -> {
                    val xdg = System.getenv("XDG_CONFIG_HOME")
                    val base = if (xdg.isNullOrBlank()) File(home, ".config") else File(xdg)
                    File(base, "devtoolbox")
                }
            }
            return File(dir, "state.json")
        }
    }
}

actual fun createStateStore(): StateStore = FileStateStore(FileStateStore.defaultFile())
