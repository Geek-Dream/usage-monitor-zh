package com.usagemonitor.presentation.ui.components

import com.usagemonitor.presentation.ui.localized

import com.usagemonitor.domain.entity.ApiSource
import com.usagemonitor.domain.entity.AppLanguage

/**
 * Selo de origem das fontes lidas localmente (Gemini CLI, Cursor, Antigravity), ou
 * `null` para as demais. Texto de tela: saiu do domain, que só classifica.
 *
 * `when` exaustivo: fonte nova não compila sem decidir se tem selo.
 */
internal fun ApiSource.statusBadgeLabel(language: AppLanguage): String? {
    val pt = language == AppLanguage.PT
    return when (this) {
        ApiSource.GEMINI -> localized(language, "Uso local", "Local usage")
        ApiSource.CURSOR -> localized(language, "Sessão local", "Local session")
        ApiSource.ANTIGRAVITY -> localized(language, "CLI local", "Local CLI")
        ApiSource.ANTHROPIC,
        ApiSource.MINIMAX,
        ApiSource.CODEX,
        ApiSource.DEEPSEEK,
        ApiSource.OPENCODE,
        ApiSource.OPENCODE_GO,
        ApiSource.KILO,
        ApiSource.OPENROUTER -> null
    }
}

/** A linha de apoio das mesmas fontes de [statusBadgeLabel], ou `null`. */
internal fun ApiSource.statusSupportingText(language: AppLanguage): String? {
    val pt = language == AppLanguage.PT
    return when (this) {
        ApiSource.GEMINI -> localized(language, "Tokens observados no Gemini CLI", "Tokens observed in Gemini CLI")
        ApiSource.CURSOR -> localized(language, "Lê o uso pela sessão já conectada no Cursor", "Reads usage through your existing Cursor session")
        ApiSource.ANTIGRAVITY -> localized(language, "Consulta /usage no Antigravity CLI já autenticado", "Reads /usage from your already authenticated Antigravity CLI")
        ApiSource.ANTHROPIC,
        ApiSource.MINIMAX,
        ApiSource.CODEX,
        ApiSource.DEEPSEEK,
        ApiSource.OPENCODE,
        ApiSource.OPENCODE_GO,
        ApiSource.KILO,
        ApiSource.OPENROUTER -> null
    }
}
