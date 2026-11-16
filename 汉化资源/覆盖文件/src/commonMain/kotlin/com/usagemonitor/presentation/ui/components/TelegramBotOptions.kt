package com.usagemonitor.presentation.ui.components

import com.usagemonitor.presentation.ui.localized

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.usagemonitor.domain.entity.AppLanguage
import com.usagemonitor.domain.entity.TELEGRAM_SUMMARY_HOURS
import com.usagemonitor.presentation.ui.theme.AppSpacing

/** O rótulo é traduzido; buscar por texto amarraria o teste ao idioma. */
const val TELEGRAM_SOURCE_CONTROL_SWITCH_TEST_TAG = "telegramSourceControlSwitch"
const val TELEGRAM_DAILY_SUMMARY_TEST_TAG = "telegramDailySummary"
const val TELEGRAM_LIVE_PANEL_SWITCH_TEST_TAG = "telegramLivePanelSwitch"
const val TELEGRAM_PC_ACTIONS_SWITCH_TEST_TAG = "telegramPcActionsSwitch"
const val TELEGRAM_MACHINE_NAME_SWITCH_TEST_TAG = "telegramMachineNameSwitch"
const val TELEGRAM_CRITICAL_SOUND_SWITCH_TEST_TAG = "telegramCriticalSoundSwitch"
const val TELEGRAM_GROUP_ALERTS_SWITCH_TEST_TAG = "telegramGroupAlertsSwitch"
const val TELEGRAM_BATCH_ALERTS_SWITCH_TEST_TAG = "telegramBatchAlertsSwitch"
const val TELEGRAM_COMPACT_PANEL_SWITCH_TEST_TAG = "telegramCompactPanelSwitch"
const val TELEGRAM_SOURCE_HEALTH_SWITCH_TEST_TAG = "telegramSourceHealthSwitch"
const val TELEGRAM_TEAM_SWITCH_TEST_TAG = "telegramTeamSwitch"
const val TELEGRAM_REMOTE_UPDATE_SWITCH_TEST_TAG = "telegramRemoteUpdateSwitch"

/**
 * Opções do que o bot faz além de responder (#398): permissões e envios por conta
 * própria. Bloco abaixo dos três passos — só faz sentido com uma conversa pareada,
 * e mantém os passos de configuração (#396, V2) como estão.
 */
@Composable
internal fun TelegramBotOptions(model: TelegramBotSectionModel, language: AppLanguage) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Text(
            text = localized(language, "Opções do bot", "Bot options"),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        TelegramOptionRow(
            label = localized(language, "Resumo diário", "Daily summary"),
            description = localized(language, "Uma mensagem por dia com a cota mais cheia de cada conta, o que reinicia hoje e o gasto das últimas 24 h. Horário em BRT; o silêncio adia.", "One message a day with each account's fullest quota, what resets today and the last 24 h spend. Time in BRT; quiet hours delay it.")
        ) {}
        val hours = listOf<Int?>(null) + TELEGRAM_SUMMARY_HOURS
        AppSegmentedControl(
            options = hours.map { hour -> AppSegment(label = hour?.let { value -> "${value.toString().padStart(2, '0')}:00" } ?: localized(language, "Desligado", "Off")) },
            selectedIndex = hours.indexOf(model.dailySummaryHour).coerceAtLeast(0),
            onSelect = { index -> model.onDailySummaryHourChange(hours[index]) },
            modifier = Modifier.testTag(TELEGRAM_DAILY_SUMMARY_TEST_TAG)
        )
        TelegramOptionRow(
            label = localized(language, "Painel fixado ao vivo", "Live pinned panel"),
            description = localized(language, "Fixa uma mensagem no topo da conversa e a edita a cada coleta, sem notificar. Desligar desafixa.", "Pins a message at the top of the chat and edits it on every collection, silently. Turning it off unpins it.")
        ) {
            AppSwitch(
                checked = model.livePanelEnabled,
                onCheckedChange = model.onLivePanelChange,
                modifier = Modifier.testTag(TELEGRAM_LIVE_PANEL_SWITCH_TEST_TAG)
            )
        }
        TelegramOptionRow(
            label = localized(language, "Permitir mudar fontes pelo bot", "Allow changing sources from the bot"),
            description = localized(language, "Com ele, o /api liga e desliga as APIs monitoradas. Desligado, o /api só lista.", "With it, /api turns monitored APIs on and off. Off, /api only lists them.")
        ) {
            AppSwitch(
                checked = model.allowSourceControl,
                onCheckedChange = model.onAllowSourceControlChange,
                modifier = Modifier.testTag(TELEGRAM_SOURCE_CONTROL_SWITCH_TEST_TAG)
            )
        }
        TelegramOptionRow(
            label = localized(language, "Permitir ações no PC", "Allow PC actions"),
            description = localized(language, "Com ele, o /hud traz a HUD para a frente neste computador. Desligado, o bot só lê.", "With it, /hud brings the HUD to the front on this computer. Off, the bot only reads.")
        ) {
            AppSwitch(
                checked = model.preferences.allowPcActions,
                onCheckedChange = { allowed -> model.onPreferencesChange(model.preferences.copy(allowPcActions = allowed)) },
                modifier = Modifier.testTag(TELEGRAM_PC_ACTIONS_SWITCH_TEST_TAG)
            )
        }
        val machine = model.machineName
        val machineDescription = localized(
            language,
            if (machine == null) {
                "Útil com o bot em mais de um computador. O sistema não informou o nome deste."
            } else {
                "Útil com o bot em mais de um computador. Este assina como $machine."
            },
            if (machine == null) {
                "Useful with the bot on more than one computer. The system did not report this one's name."
            } else {
                "Useful with the bot on more than one computer. This one signs as $machine."
            }
        )
        TelegramOptionRow(
            label = localized(language, "Nome deste PC nas mensagens", "This PC's name in messages"),
            description = machineDescription
        ) {
            AppSwitch(
                checked = model.preferences.showMachineName,
                onCheckedChange = { show -> model.onPreferencesChange(model.preferences.copy(showMachineName = show)) },
                modifier = Modifier.testTag(TELEGRAM_MACHINE_NAME_SWITCH_TEST_TAG)
            )
        }
        TelegramOptionRow(
            label = localized(language, "Mostrar o time no bot", "Show the team in the bot"),
            description = localized(language, "Com ele, o /time lista quem do time está online pelo apelido. Expõe dado de outras pessoas na conversa.", "With it, /team lists who from the team is online by nickname. It shows other people's data in the chat.")
        ) {
            AppSwitch(
                checked = model.preferences.showTeam,
                onCheckedChange = { show -> model.onPreferencesChange(model.preferences.copy(showTeam = show)) },
                modifier = Modifier.testTag(TELEGRAM_TEAM_SWITCH_TEST_TAG)
            )
        }
        TelegramOptionRow(
            label = localized(language, "Permitir atualizar pelo bot", "Allow updating from the bot"),
            description = localized(language, "Avisa quando uma versão nova já foi baixada e oferece \"Atualizar e reiniciar o app\": o app fecha e reabre neste PC.", "Says when a new version is downloaded and offers \"Update and restart the app\": the app closes and reopens on this PC.")
        ) {
            AppSwitch(
                checked = model.preferences.allowRemoteUpdate,
                onCheckedChange = { allowed -> model.onPreferencesChange(model.preferences.copy(allowRemoteUpdate = allowed)) },
                modifier = Modifier.testTag(TELEGRAM_REMOTE_UPDATE_SWITCH_TEST_TAG)
            )
        }
        TelegramOptionRow(
            label = localized(language, "Só alerta crítico toca", "Only critical alerts ring"),
            description = localized(language, "Cota em 90% ou mais toca no celular; os outros alertas chegam em silêncio, sem vibrar.", "A quota at 90% or more rings the phone; the other alerts arrive silently, without vibrating.")
        ) {
            AppSwitch(
                checked = model.preferences.soundOnlyOnCritical,
                onCheckedChange = { only -> model.onPreferencesChange(model.preferences.copy(soundOnlyOnCritical = only)) },
                modifier = Modifier.testTag(TELEGRAM_CRITICAL_SOUND_SWITCH_TEST_TAG)
            )
        }
        TelegramOptionRow(
            label = localized(language, "Agrupar alertas repetidos", "Group repeated alerts"),
            description = localized(language, "O mesmo alerta no mesmo dia edita a mensagem anterior com um contador, sem vibrar de novo. Crítico sempre abre outra.", "The same alert on the same day edits the previous message with a counter, without vibrating again. Critical always opens a new one.")
        ) {
            AppSwitch(
                checked = model.preferences.groupRepeatedAlerts,
                onCheckedChange = { group -> model.onPreferencesChange(model.preferences.copy(groupRepeatedAlerts = group)) },
                modifier = Modifier.testTag(TELEGRAM_GROUP_ALERTS_SWITCH_TEST_TAG)
            )
        }
        TelegramOptionRow(
            label = localized(language, "Juntar alertas", "Batch alerts"),
            description = localized(language, "Os alertas de 15 min chegam numa mensagem só, um tópico por alerta. Crítico sai na hora.", "Alerts from 15 min arrive in a single message, one bullet per alert. Critical goes out right away.")
        ) {
            AppSwitch(
                checked = model.preferences.batchAlerts,
                onCheckedChange = { batch -> model.onPreferencesChange(model.preferences.copy(batchAlerts = batch)) },
                modifier = Modifier.testTag(TELEGRAM_BATCH_ALERTS_SWITCH_TEST_TAG)
            )
        }
        TelegramOptionRow(
            label = localized(language, "Painel em uma linha", "One-line panel"),
            description = localized(language, "O painel fixado vira uma linha com o risco e o pior percentual de cada conta, que cabe no topo da conversa.", "The pinned panel becomes one line with each account's risk and worst percentage, which fits the top of the chat.")
        ) {
            AppSwitch(
                checked = model.preferences.compactPanel,
                onCheckedChange = { compact -> model.onPreferencesChange(model.preferences.copy(compactPanel = compact)) },
                modifier = Modifier.testTag(TELEGRAM_COMPACT_PANEL_SWITCH_TEST_TAG)
            )
        }
        TelegramOptionRow(
            label = localized(language, "Avisar fonte com falha", "Warn about a failing source"),
            description = localized(language, "Depois de 15 min falhando, o bot diz o motivo e de quando são os números. Avisa de novo quando ela voltar.", "After 15 min failing, the bot says why and how old the numbers are. It tells you again when it is back.")
        ) {
            AppSwitch(
                checked = model.preferences.sourceHealthAlerts,
                onCheckedChange = { warn -> model.onPreferencesChange(model.preferences.copy(sourceHealthAlerts = warn)) },
                modifier = Modifier.testTag(TELEGRAM_SOURCE_HEALTH_SWITCH_TEST_TAG)
            )
        }
    }
}

/** Rótulo em mono, descrição em sans e o controle à direita — o padrão de `SettingsOptionRow`, sem a linha de dados. */
@Composable
internal fun TelegramOptionRow(label: String, description: String, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        control()
    }
}
