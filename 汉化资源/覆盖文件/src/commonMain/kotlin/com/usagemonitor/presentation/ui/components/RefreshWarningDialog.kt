package com.usagemonitor.presentation.ui.components

import com.usagemonitor.presentation.ui.localized

import com.usagemonitor.presentation.ui.CommonLabels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.usagemonitor.domain.entity.AppLanguage

@Composable
fun RefreshWarningDialog(
    language: AppLanguage,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val title = localized(language, "Atualizar agora?", "Refresh now?")
    val body = localized(language, "Forçar uma atualização antes do horário agendado consome cota da API e pode acelerar o esgotamento do limite de requisições. Deseja continuar?", "Forcing a refresh ahead of the scheduled time consumes API quota and may accelerate hitting the request limit. Continue?")
    val confirmLabel = localized(language, "Atualizar", "Refresh")
    val dismissLabel = CommonLabels.cancel(language)

    // Os botões do app, sendo o de confirmar o único com peso, porque é a ação
    // que o diálogo propõe. Escurecimento, foco preso e Esc vêm do `AppDialog`.
    AppDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
        },
        text = {
            ModalDialogText(text = body)
        },
        confirmButton = {
            AppButton(label = confirmLabel, onClick = onConfirm, tone = AppButtonTone.PRIMARY)
        },
        dismissButton = {
            AppButton(label = dismissLabel, onClick = onDismiss, tone = AppButtonTone.GHOST)
        }
    )
}
