package com.usagemonitor

import com.russhwolf.settings.Settings
import com.usagemonitor.domain.entity.AppLanguage
import com.usagemonitor.presentation.ui.LocalizationState

/**
 * Idioma efetivo do processo: o `--locale` desta execução (issue #418) ou, sem
 * ele, a preferência gravada.
 *
 * Um dono só, porque o idioma é lido fora da composição — exportação, bot do
 * Telegram, mensagens das Configurações — e cada leitura direta da preferência
 * ignoraria o `--locale`: a HUD em inglês e o relatório em português.
 *
 * O `--locale` nunca é gravado. Escolher um idioma nas Configurações grava a
 * escolha e encerra o override: a partir dali vale o que o usuário escolheu.
 */
internal class LanguageSelection(
    private val settings: Settings,
    initialOverride: AppLanguage?
) {
    @Volatile
    private var override: AppLanguage? = initialOverride

    fun current(): AppLanguage = (override ?: storedLanguage(settings)).also { language ->
        LocalizationState.language = language
    }

    fun choose(selected: AppLanguage) {
        override = null
        LocalizationState.language = selected
        settings.putString(LANGUAGE_KEY, selected.name)
    }
}
