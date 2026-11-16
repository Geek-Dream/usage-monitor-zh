package com.usagemonitor.presentation.ui.components

import com.usagemonitor.presentation.ui.localized

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.usagemonitor.domain.entity.ApiSource
import com.usagemonitor.domain.entity.AppLanguage
import com.usagemonitor.presentation.ui.theme.AccountAccent
import com.usagemonitor.presentation.ui.theme.AccountEmoji
import com.usagemonitor.presentation.ui.theme.AppAccents
import com.usagemonitor.presentation.ui.theme.AppSpacing

@Composable
internal fun AnthropicAccountsTab(
    currentLanguage: AppLanguage,
    anthropicProfiles: List<AnthropicProfileUiModel>,
    expandedProfileId: String?,
    onAnthropicProfileToggle: (String, Boolean) -> Unit,
    onAnthropicProfileRename: (String, String) -> Unit,
    onAnthropicProfileColorChange: (String, AccountAccent?) -> Unit,
    onAnthropicProfileEmojiChange: (String, AccountEmoji?) -> Unit,
    onAddAnthropicProfile: () -> Unit,
    onRemoveAnthropicProfile: (String) -> Unit,
    onRescanAnthropicProfiles: () -> Unit,
    onToggleProfileExpanded: (String) -> Unit,
    codexAccounts: CodexAccountsSettings = CodexAccountsSettings()
) {
    // As duas ações vão para o `trailing` do cabeçalho, como no protótipo: elas
    // agem sobre a lista inteira, e no corpo competiam com as linhas de perfil.
    AppDataSurfaceFlush(
        header = {
            AppSectionHeader(
                title = localized(currentLanguage, "Contas Anthropic", "Anthropic accounts"),
                trailing = {
                    AppButton(
                        label = localized(currentLanguage, "Redetectar", "Rescan"),
                        onClick = onRescanAnthropicProfiles,
                        tone = AppButtonTone.GHOST
                    )
                    AppButton(
                        label = localized(currentLanguage, "Adicionar", "Add"),
                        onClick = onAddAnthropicProfile,
                        tone = AppButtonTone.PRIMARY
                    )
                }
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            if (anthropicProfiles.isEmpty()) {
                Text(
                    text = localized(currentLanguage, "Nenhum perfil Anthropic detectado.", "No Anthropic profile detected."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                anthropicProfiles.forEach { profile ->
                    key(profile.id) {
                        AnthropicProfileRow(
                            profile = profile,
                            language = currentLanguage,
                            expanded = profile.id == expandedProfileId,
                            onToggle = onAnthropicProfileToggle,
                            onRename = onAnthropicProfileRename,
                            onColorChange = onAnthropicProfileColorChange,
                            onEmojiChange = onAnthropicProfileEmojiChange,
                            onRemove = onRemoveAnthropicProfile,
                            onToggleExpanded = { onToggleProfileExpanded(profile.id) }
                        )
                    }
                }
            }
        }
    }
    // Issue #329: abaixo das contas Anthropic, na mesma aba.
    CodexAccountsSection(language = currentLanguage, settings = codexAccounts)
}

/**
 * Navegação lateral das Configurações.
 *
 * Era uma fileira de chips presa no topo. Cinco chips numa janela estreita
 * quebravam em duas linhas e empurravam o conteúdo para baixo; na lateral eles
 * ocupam largura fixa e a lista cresce sem mexer no que está sendo lido.
 *
 * Continua sendo o **enum existente**: nenhuma seção nova, nenhum valor novo em
 * `SettingsTab`, e a `testTag` de cada uma é a mesma.
 */
@Composable
private fun AnthropicProfileRow(
    profile: AnthropicProfileUiModel,
    language: AppLanguage,
    expanded: Boolean,
    onToggle: (String, Boolean) -> Unit,
    onRename: (String, String) -> Unit,
    onColorChange: (String, AccountAccent?) -> Unit,
    onEmojiChange: (String, AccountEmoji?) -> Unit,
    onRemove: (String) -> Unit,
    onToggleExpanded: () -> Unit
) {
    val statusText = when (profile.status) {
        AnthropicProfileUiStatus.READY -> localized(language, "Pronto", "Ready")
        AnthropicProfileUiStatus.INCOMPLETE -> localized(language, "Incompleto", "Incomplete")
        AnthropicProfileUiStatus.INVALID -> localized(language, "Inválido", "Invalid")
        AnthropicProfileUiStatus.DUPLICATE -> localized(language, "Conta duplicada", "Duplicate account")
    }
    val statusTone = if (profile.status == AnthropicProfileUiStatus.READY) {
        AppTone.OK
    } else {
        AppTone.CRITICAL
    }
    val editLabel = localized(language, "Editar", "Edit")
    val collapseLabel = localized(language, "Recolher", "Collapse")

    // Linha de dados, não bloco em `surfaceVariant`: aquele é o realce de hover
    // das listas, e com ele como fundo fixo passar o mouse deixava de dar
    // retorno. O estado do perfil vira ponto e palavra, como no resto do app.
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            AppDataRow(showDivider = false, horizontalPadding = 0.dp) {
                // O marcador de 2dp na cor da conta — o mesmo do card, que é onde
                // a cor escolhida aparece. Sem cor própria, o acento da Anthropic.
                AppSourceMarker(
                    color = profile.color?.current ?: accentColorFor(ApiSource.ANTHROPIC, AppAccents.current),
                    height = 28.dp
                )
                // O emoji da conta (issue #287) antes do apelido, como no card.
                val emoji = profile.emoji
                if (emoji != null) {
                    AccountEmojiGlyph(emoji = emoji, size = 18.dp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val identity = profile.identityLabel
                    if (identity != null) {
                        Text(
                            text = identity,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                AppStatusIndicator(label = statusText, tone = statusTone)
                AppSwitch(
                    checked = profile.enabled,
                    onCheckedChange = { checked -> onToggle(profile.id, checked) }
                )
                AppIconButton(
                    contentDescription = if (expanded) collapseLabel else editLabel,
                    onClick = onToggleExpanded
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.Close else Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AppExpandable(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                        DebouncedTextField(
                            value = profile.label,
                            label = localized(language, "Apelido", "Label"),
                            onCommit = { newLabel -> onRename(profile.id, newLabel) }
                        )
                        AccountColorPicker(
                            profileId = profile.id,
                            selected = profile.color,
                            language = language,
                            onSelect = { color -> onColorChange(profile.id, color) }
                        )
                        AccountEmojiPicker(
                            profileId = profile.id,
                            selected = profile.emoji,
                            language = language,
                            onSelect = { emoji -> onEmojiChange(profile.id, emoji) }
                        )
                        Text(
                            text = profile.path,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = listOfNotNull(statusText, profile.detail).joinToString(" — "),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusTone.color()
                        )
                        if (profile.removable) {
                            AppButton(
                                label = localized(language, "Remover do monitor", "Remove from monitor"),
                                onClick = { onRemove(profile.id) },
                                tone = AppButtonTone.GHOST
                            )
                        }
                }
            }
        }
    }
}

/**
 * A cor da conta (issue #275): "Padrão" mais as oito de [AccountAccent], em
 * amostras rotuladas. Paleta fixa e não seletor livre — cada cor tem variante
 * clara e escura com contraste medido, e é ela que o card e a HUD pintam no
 * marcador e na marca do fornecedor.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountColorPicker(
    profileId: String,
    selected: AccountAccent?,
    language: AppLanguage,
    onSelect: (AccountAccent?) -> Unit
) {
    val isPt = language == AppLanguage.PT
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Text(
            text = localized(language, "Cor", "Color"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            AppSwatchChip(
                label = localized(language, "Padrão", "Default"),
                swatch = null,
                selected = selected == null,
                onClick = { onSelect(null) },
                modifier = Modifier.testTag("$ACCOUNT_COLOR_OPTION_TEST_TAG_PREFIX${profileId}_DEFAULT")
            )
            AccountAccent.entries.forEach { accent ->
                AppSwatchChip(
                    label = accent.label(isPt),
                    swatch = accent.current,
                    selected = selected == accent,
                    onClick = { onSelect(accent) },
                    modifier = Modifier.testTag("$ACCOUNT_COLOR_OPTION_TEST_TAG_PREFIX${profileId}_${accent.name}")
                )
            }
        }
    }
}

/**
 * O emoji da conta (issue #287): "Nenhum" mais os de [AccountEmoji], só o glifo
 * em cada opção — o nome vai na semântica. Conjunto fixo pelo mesmo motivo da
 * paleta de cores: cada glifo foi visto renderizado, e um campo livre traria
 * sequências que viram quadrado vazio onde falta a fonte.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountEmojiPicker(
    profileId: String,
    selected: AccountEmoji?,
    language: AppLanguage,
    onSelect: (AccountEmoji?) -> Unit
) {
    val isPt = language == AppLanguage.PT
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Text(
            text = "Emoji",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            val none = localized(language, "Nenhum", "None")
            AppGlyphChip(
                description = none,
                selected = selected == null,
                onClick = { onSelect(null) },
                modifier = Modifier.testTag("$ACCOUNT_EMOJI_OPTION_TEST_TAG_PREFIX${profileId}_NONE")
            ) {
                Text(
                    text = none,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            AccountEmoji.entries.forEach { emoji ->
                AppGlyphChip(
                    description = emoji.label(isPt),
                    selected = selected == emoji,
                    onClick = { onSelect(emoji) },
                    modifier = Modifier.testTag("$ACCOUNT_EMOJI_OPTION_TEST_TAG_PREFIX${profileId}_${emoji.name}")
                ) {
                    AccountEmojiGlyph(emoji = emoji, size = 18.dp)
                }
            }
        }
    }
}
