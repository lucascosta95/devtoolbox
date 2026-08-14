package dev.devtoolbox.core.persistence

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PersistedState(
    @SerialName("selected_id") val selectedId: String? = null,
    val favorites: List<String> = emptyList(),
    val theme: String? = null,
    val accent: String? = null,
    @SerialName("update_skipped_version") val skippedVersion: String? = null,
    val version: Int = 1,
)

object StateCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun encode(state: PersistedState): String = json.encodeToString(state)

    fun decode(text: String): PersistedState? =
        runCatching { json.decodeFromString<PersistedState>(text) }.getOrNull()
}

interface StateStore {
    fun load(): PersistedState?
    fun save(state: PersistedState)
}

class InMemoryStateStore(private var state: PersistedState? = null) : StateStore {
    var saveCount: Int = 0
        private set

    override fun load(): PersistedState? = state

    override fun save(state: PersistedState) {
        this.state = state
        saveCount++
    }
}

object NoOpStateStore : StateStore {
    override fun load(): PersistedState? = null
    override fun save(state: PersistedState) = Unit
}
