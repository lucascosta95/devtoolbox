package dev.devtoolbox.core.persistence

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * O que sobrevive entre execuções.
 *
 * Não guarda as entradas das ferramentas: elas costumam conter dados de trabalho (tokens,
 * payloads) que não deveriam ficar em disco sem o usuário pedir.
 */
@Serializable
data class PersistedState(
    @SerialName("selected_id") val selectedId: String? = null,
    val favorites: List<String> = emptyList(),
    val recent: List<String> = emptyList(),
    /** "dark" ou "light". */
    val theme: String? = null,
    /** Versão do formato — permite migrar sem quebrar instalações antigas. */
    val version: Int = 1,
)

/** Serialização isolada aqui para que a camada de plataforma só cuide de arquivo. */
object StateCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun encode(state: PersistedState): String = json.encodeToString(state)

    /** Devolve `null` em vez de lançar: um arquivo corrompido não pode impedir o app de abrir. */
    fun decode(text: String): PersistedState? =
        runCatching { json.decodeFromString<PersistedState>(text) }.getOrNull()
}

/**
 * Leitura e escrita do estado. A implementação real é por plataforma; os testes usam
 * [InMemoryStateStore].
 */
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

/** Usado quando não há onde persistir — o app funciona, só não lembra nada. */
object NoOpStateStore : StateStore {
    override fun load(): PersistedState? = null
    override fun save(state: PersistedState) = Unit
}
