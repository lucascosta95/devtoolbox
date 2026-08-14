package dev.devtoolbox.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.devtoolbox.core.BuildInfo
import dev.devtoolbox.core.update.Release
import dev.devtoolbox.ds.Nocturne
import dev.devtoolbox.ds.components.CardKicker
import dev.devtoolbox.ds.components.GhostButton
import dev.devtoolbox.ds.components.ModalDialog
import dev.devtoolbox.ds.components.OutlinedButton
import dev.devtoolbox.ds.components.PhosphorIcon
import dev.devtoolbox.ds.components.SecondaryButton
import dev.devtoolbox.ds.components.Text

@Composable
fun UpdateDialog(
    notice: UpdateNotice,
    onOpenRelease: (Release) -> Unit,
    onRemindLater: () -> Unit,
    onSkipVersion: (Release) -> Unit,
) {
    ModalDialog(onDismiss = onRemindLater) {
        when (notice) {
            is UpdateNotice.Available ->
                AvailableBody(notice.release, onOpenRelease, onRemindLater, onSkipVersion)
            UpdateNotice.UpToDate -> UpToDateBody(onRemindLater)
        }
    }
}

@Composable
private fun AvailableBody(
    release: Release,
    onOpenRelease: (Release) -> Unit,
    onRemindLater: () -> Unit,
    onSkipVersion: (Release) -> Unit,
) {
    Header(icon = "arrows-clockwise", kicker = "Atualização disponível")

    Text(
        "DevToolbox v${release.version ?: release.tag}",
        style = Nocturne.type.toolTitle,
        modifier = Modifier.padding(top = Nocturne.space.sm),
    )

    Text(
        "Você está usando a ${BuildInfo.displayVersion}. Uma versão mais nova foi publicada no GitHub.",
        style = Nocturne.type.body,
        color = Nocturne.colors.text(0.65f),
        modifier = Modifier.padding(top = Nocturne.space.xs),
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Nocturne.space.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        GhostButton(label = "Pular esta versão", onClick = { onSkipVersion(release) })

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Nocturne.space.sm),
        ) {
            SecondaryButton(label = "Lembrar depois", onClick = onRemindLater)
            OutlinedButton(
                label = "Ver no GitHub",
                icon = "arrow-right",
                onClick = { onOpenRelease(release) },
            )
        }
    }
}

@Composable
private fun UpToDateBody(onClose: () -> Unit) {
    Header(icon = "check-circle", kicker = "Tudo em dia", fill = true)

    Text(
        "DevToolbox ${BuildInfo.displayVersion}",
        style = Nocturne.type.toolTitle,
        modifier = Modifier.padding(top = Nocturne.space.sm),
    )

    Text(
        "Você já está na versão mais recente.",
        style = Nocturne.type.body,
        color = Nocturne.colors.text(0.65f),
        modifier = Modifier.padding(top = Nocturne.space.xs),
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Nocturne.space.lg),
        horizontalArrangement = Arrangement.End,
    ) {
        SecondaryButton(label = "Fechar", onClick = onClose)
    }
}

@Composable
private fun Header(icon: String, kicker: String, fill: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Nocturne.space.xs),
    ) {
        PhosphorIcon(icon, size = 13.dp, tint = Nocturne.colors.onAccentSurface, fill = fill)
        Column(Modifier.weight(1f)) { CardKicker(kicker) }
    }
}
