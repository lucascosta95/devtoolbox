package dev.devtoolbox.ui

import dev.devtoolbox.core.persistence.StateStore

/**
 * Cria o repositório de estado da plataforma corrente.
 *
 * No desktop grava um JSON no diretório de configuração do SO; em targets futuros
 * (Android, iOS, Wasm) cada um traz o seu `actual`.
 */
expect fun createStateStore(): StateStore
