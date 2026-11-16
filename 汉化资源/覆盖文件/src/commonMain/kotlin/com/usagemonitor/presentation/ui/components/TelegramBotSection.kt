package com.usagemonitor.presentation.ui.components

import com.usagemonitor.presentation.ui.localized

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.usagemonitor.domain.entity.AppLanguage
import com.usagemonitor.domain.entity.TelegramBotPreferences
import com.usagemonitor.domain.entity.TelegramChat
import com.usagemonitor.presentation.ui.theme.AppSpacing
import kotlinx.coroutines.delay

const val TELEGRAM_SECTION_TEST_TAG = "telegramSection"
const val TELEGRAM_SWITCH_TEST_TAG = "telegramSwitch"

/** Interruptor "Controle" de cada conversa (#408, AA21); sufixo é o id da conversa. */
const val TELEGRAM_CHAT_CONTROL_TEST_TAG_PREFIX = "telegramChatControl-"

/**
 * O que a seção do bot mostra (#387; #396, direção V2 — três passos).
 * [statusPreview] é a resposta real que o `/status` daria agora, sem as marcas HTML.
 */
class TelegramBotSectionModel(
    val enabled: Boolean,
    val token: String,
    val statusLabel: String,
    val statusTone: AppTone,
    val chats: List<TelegramChat>,
    /** Código em aberto e quanto falta para expirar, já em texto. */
    val pairingCode: String?,
    val pairingHint: String?,
    val statusPreview: String,
    val onEnabledChange: (Boolean) -> Unit,
    val onTokenChange: (String) -> Unit,
    val onStartPairing: () -> Unit,
    val onRemoveChat: (Long) -> Unit,
    val onSendTest: () -> Unit,
    /** O polling está de pé com este token: o passo do token está feito. */
    val connected: Boolean = false,
    /** `@` do bot sem a arroba, do `getMe`; `null` esconde "Abrir no Telegram". */
    val botUsername: String? = null,
    /** `/api` pode ligar e desligar fontes (#398, Y8). */
    val allowSourceControl: Boolean = false,
    val onAllowSourceControlChange: (Boolean) -> Unit = {},
    /** Hora do resumo diário (#398, Y1); `null` desligado. */
    val dailySummaryHour: Int? = null,
    val onDailySummaryHourChange: (Int?) -> Unit = {},
    /** Painel fixado que se atualiza sozinho (#398, Y5). */
    val livePanelEnabled: Boolean = false,
    val onLivePanelChange: (Boolean) -> Unit = {},
    /** Papel de cada conversa (#408, AA21): `true` dá controle, `false` deixa só leitura. */
    val onChatControlChange: (Long, Boolean) -> Unit = { _, _ -> },
    /** Opções da #408; o card muda uma e devolve o conjunto. */
    val preferences: TelegramBotPreferences = TelegramBotPreferences(),
    val onPreferencesChange: (TelegramBotPreferences) -> Unit = {},
    /** Nome deste PC (#408, AA20); `null` quando o sistema não informa. */
    val machineName: String? = null
)

/**
 * Seção dentro da aba Alertas — sem aba nova (`SettingsTab` não ganha valor):
 * o bot é mais um destino dos mesmos alertas, e os comandos dele mudam estas
 * mesmas preferências.
 *
 * Três passos numerados (#396, direção V2): token, pareamento e teste. O comando
 * de pareamento é uma caixa com **Copiar** e, quando o `getMe` deu o `@` do bot,
 * **Abrir no Telegram** — o link `t.me/<bot>?start=<código>` já manda o `/start`.
 * A conversa de exemplo saiu do card: "Ver como o bot responde" a abre, recolhida.
 */
@Composable
fun TelegramBotSection(model: TelegramBotSectionModel, language: AppLanguage, modifier: Modifier = Modifier) {
    val pt = language == AppLanguage.PT
    AppDataSurfaceFlush(
        modifier = modifier.fillMaxWidth().testTag(TELEGRAM_SECTION_TEST_TAG),
        header = {
            AppSectionHeader(
                title = localized(language, "Bot do Telegram", "Telegram bot"),
                trailing = {
                    AppSwitch(checked = model.enabled, onCheckedChange = model.onEnabledChange, modifier = Modifier.testTag(TELEGRAM_SWITCH_TEST_TAG))
                }
            )
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(AppSpacing.md), verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
            BodyText(
                localized(language, "Recebe os alertas no Telegram e responde a comandos. Só metadados de uso — nunca prompt ou resposta.", "Receives alerts on Telegram and answers commands. Usage metadata only — never prompts or responses.")
            )
            TelegramStep(number = 1, title = localized(language, "Token do bot", "Bot token"), state = stepState(done = model.enabled && model.connected), language = language) {
                BodyText(localized(language, "Crie o bot com o @BotFather e cole o token aqui.", "Create the bot with @BotFather and paste its token here."))
                AppTextField(
                    value = model.token,
                    onValueChange = model.onTokenChange,
                    placeholder = localized(language, "Token do bot", "Bot token"),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (model.enabled) AppStatusIndicator(label = model.statusLabel, tone = model.statusTone)
            }
            if (!model.enabled) {
                return@Column
            }
            val pairingDone = model.chats.isNotEmpty() && model.pairingCode == null
            TelegramStep(number = 2, title = localized(language, "Parear uma conversa", "Pair a chat"), state = stepState(done = pairingDone), language = language) {
                TelegramChats(model, language)
                val code = model.pairingCode
                if (code != null) {
                    TelegramPairingCommand(code, model, language)
                } else {
                    AppButton(
                        label = if (model.chats.isEmpty()) (localized(language, "Parear conversa", "Pair chat")) else (localized(language, "Parear outra conversa", "Pair another chat")),
                        onClick = model.onStartPairing
                    )
                }
            }
            TelegramStep(number = 3, title = localized(language, "Testar", "Test"), state = stepState(done = false, waiting = model.chats.isEmpty()), language = language) {
                if (model.chats.isEmpty()) {
                    BodyText(localized(language, "Disponível depois do pareamento.", "Available after pairing."))
                }
                AppButton(label = localized(language, "Enviar teste", "Send test"), onClick = model.onSendTest, enabled = model.chats.isNotEmpty())
            }
            TelegramBotOptions(model, language)
            TelegramPreview(model.statusPreview, language)
            BodyText(
                localized(language, "Discord fica para uma segunda fase: exige conexão permanente (Gateway).", "Discord is a second phase: it needs a persistent connection (Gateway).")
            )
        }
    }
}

private enum class StepState { DONE, CURRENT, WAITING }

private fun stepState(done: Boolean, waiting: Boolean = false): StepState = when {
    done -> StepState.DONE
    waiting -> StepState.WAITING
    else -> StepState.CURRENT
}

/** Um passo: número e título, o estado em ponto e palavra, e o conteúdo abaixo. */
@Composable
private fun TelegramStep(number: Int, title: String, state: StepState, language: AppLanguage, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Text("$number · $title", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            when (state) {
                StepState.DONE -> AppStatusIndicator(label = localized(language,  "Feito", "Done"), tone = AppTone.OK)
                StepState.CURRENT -> AppStatusIndicator(label = localized(language,  "Agora", "Now"), tone = AppTone.INFO)
                StepState.WAITING -> AppStatusIndicator(label = localized(language,  "Depois", "Later"), tone = AppTone.NEUTRAL)
            }
        }
        content()
    }
}

/**
 * Conversas pareadas; "Remover" colado ao nome, não na outra ponta da coluna. O
 * interruptor "Controle" (#408, AA21) separa a conversa que muda alertas e fontes da
 * que só lê — um grupo da equipe, por exemplo.
 */
@Composable
private fun TelegramChats(model: TelegramBotSectionModel, language: AppLanguage) {
    if (model.chats.isEmpty()) {
        BodyText(localized(language,  "Nenhuma conversa pareada ainda.", "No paired chat yet."))
        return
    }
    model.chats.forEach { chat ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Text(chat.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                if (chat.readOnly) (localized(language,  "Só leitura", "Read only")) else (localized(language,  "Controle", "Control")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AppSwitch(
                checked = !chat.readOnly,
                onCheckedChange = { control -> model.onChatControlChange(chat.id, control) },
                modifier = Modifier.testTag(TELEGRAM_CHAT_CONTROL_TEST_TAG_PREFIX + chat.id)
            )
            AppButton(label = localized(language,  "Remover", "Remove"), onClick = { model.onRemoveChat(chat.id) }, tone = AppButtonTone.GHOST)
        }
    }
}

/** Caixa do comando com Copiar (vira "Copiado ✓" por 1,6 s) e o link que já manda o `/start`. */
@Composable
private fun TelegramPairingCommand(code: String, model: TelegramBotSectionModel, language: AppLanguage) {
    val command = "/start $code"
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_FEEDBACK_MILLIS)
            copied = false
        }
    }
    Text(localized(language,  "Envie ao bot no Telegram", "Send this to the bot on Telegram"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    AppDataSurface(contentPadding = AppSpacing.sm) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Text(command, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            AppButton(
                label = if (copied) (localized(language,  "Copiado ✓", "Copied ✓")) else (localized(language,  "Copiar", "Copy")),
                onClick = {
                    clipboard.setText(AnnotatedString(command))
                    copied = true
                },
                tone = AppButtonTone.PRIMARY
            )
        }
    }
    model.pairingHint?.let { hint -> BodyText(hint) }
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        model.botUsername?.let { username ->
            AppButton(
                label = localized(language,  "Abrir no Telegram", "Open in Telegram"),
                onClick = { runCatching { uriHandler.openUri("https://t.me/$username?start=$code") } }
            )
        }
        AppButton(label = localized(language,  "Gerar novo código", "New code"), onClick = model.onStartPairing, tone = AppButtonTone.GHOST)
    }
}

/** A resposta real do `/status`, recolhida por padrão. */
@Composable
private fun TelegramPreview(preview: String, language: AppLanguage) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        AppButton(
            label = if (expanded) (localized(language,  "▾ Ocultar exemplo", "▾ Hide sample")) else (localized(language,  "▸ Ver como o bot responde", "▸ See how the bot replies")),
            onClick = { expanded = !expanded },
            tone = AppButtonTone.GHOST
        )
        AppExpandable(expanded) {
            AppDataSurface(contentPadding = AppSpacing.sm) {
                Text("/status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(preview, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun BodyText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private const val COPIED_FEEDBACK_MILLIS = 1_600L
