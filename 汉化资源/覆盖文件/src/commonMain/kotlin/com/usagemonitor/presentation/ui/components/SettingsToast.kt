package com.usagemonitor.presentation.ui.components

import com.usagemonitor.presentation.ui.localized

import com.usagemonitor.domain.entity.AppLanguage

/** Campo das Configurações a que um aviso de gravação se refere. */
enum class SettingsField {
    THEME,
    LANGUAGE,
    AUTO_START,
    WINDOW_OPACITY,
    UI_SCALE,
    ALERTS,
    MONITORED_APIS,
    API_KEY,
    ANTHROPIC_PROFILES,
    ANTHROPIC_PROFILE_LABEL,
    TEAM_INTEGRATION,
    TEAM_SERVER,
    TEAM_KEY,
    TEAM_ALIAS,
    TEAM_ACCOUNTS,
    TEAM_ADMIN_TOKEN,
    NETWORK_PROXY
}

/**
 * Aviso mostrado no rodapé do diálogo de Configurações.
 *
 * Mesma anatomia de [com.usagemonitor.presentation.viewmodel.DashboardToast]: o
 * tipo carrega só o fato, e a tradução acontece na borda da UI. O diálogo é uma
 * janela separada do dashboard, então ele precisa do próprio host — o
 * `SnackbarHost` da janela principal não desenha por cima dela.
 */
sealed interface SettingsToast {
    /** A alteração chegou ao disco. */
    data class Saved(val field: SettingsField) : SettingsToast

    /** A gravação falhou; o aviso não pode dizer que salvou. */
    data class SaveFailed(val field: SettingsField) : SettingsToast

    /** Apelido do time em branco: não grava e o campo volta ao valor anterior. */
    data object TeamAliasRequired : SettingsToast
}

/**
 * Par [SettingsToast] + identificador de emissão.
 *
 * O id é o que faz dois avisos iguais seguidos aparecerem duas vezes: sem ele o
 * `LaunchedEffect` da tela não reagiria ao segundo, por ser um valor igual.
 */
data class SettingsToastEvent(
    val id: Int,
    val toast: SettingsToast
)

/**
 * Frase completa por campo, em vez de "$campo salvo".
 *
 * Em português o particípio concorda com o gênero do campo ("Tema salvo" mas
 * "Opacidade salva"), e montar a frase por concatenação erraria metade dos
 * casos. A tabela é maior e não tem como sair errada.
 */
fun settingsToastMessage(toast: SettingsToast, language: AppLanguage): String {
    return when (toast) {
        is SettingsToast.Saved -> savedMessage(toast.field, language)
        is SettingsToast.SaveFailed -> localized(
            language,
            "Falha ao salvar: ${fieldLabel(toast.field, AppLanguage.PT)}",
            "Could not save: ${fieldLabel(toast.field, AppLanguage.EN)}"
        )
        SettingsToast.TeamAliasRequired -> localized(language, "O apelido não pode ficar vazio.", "The alias cannot be empty.")
    }
}

private fun savedMessage(field: SettingsField, language: AppLanguage): String {
    val pt = when (field) {
        SettingsField.THEME -> "Tema salvo"
        SettingsField.LANGUAGE -> "Idioma salvo"
        SettingsField.AUTO_START -> "Inicialização com sistema salva"
        SettingsField.WINDOW_OPACITY -> "Opacidade salva"
        SettingsField.UI_SCALE -> "Tamanho da interface salvo"
        SettingsField.ALERTS -> "Preferências de alerta salvas"
        SettingsField.MONITORED_APIS -> "APIs monitoradas salvas"
        SettingsField.API_KEY -> "Chave de API salva"
        SettingsField.ANTHROPIC_PROFILES -> "Contas Anthropic salvas"
        SettingsField.ANTHROPIC_PROFILE_LABEL -> "Apelido da conta salvo"
        SettingsField.TEAM_INTEGRATION -> "Integração com time salva"
        SettingsField.TEAM_SERVER -> "Servidor do time salvo"
        SettingsField.TEAM_KEY -> "Chave salva"
        SettingsField.TEAM_ALIAS -> "Apelido salvo"
        SettingsField.TEAM_ACCOUNTS -> "Contas do time salvas"
        SettingsField.TEAM_ADMIN_TOKEN -> "Token de administração salvo"
        SettingsField.NETWORK_PROXY -> "Proxy salvo"
    }
    val en = if (field == SettingsField.TEAM_KEY) {
        "Key saved"
    } else {
        "${fieldLabel(field, AppLanguage.EN)} saved"
    }
    return localized(language, pt, en)
}

private fun fieldLabel(field: SettingsField, language: AppLanguage): String {
    return when (field) {
        SettingsField.THEME -> localized(language, "tema", "Theme")
        SettingsField.LANGUAGE -> localized(language, "idioma", "Language")
        SettingsField.AUTO_START -> localized(language, "inicialização com sistema", "System startup")
        SettingsField.WINDOW_OPACITY -> localized(language, "opacidade da janela", "Window opacity")
        SettingsField.UI_SCALE -> localized(language, "tamanho da interface", "Interface size")
        SettingsField.ALERTS -> localized(language, "preferências de alerta", "Alert preferences")
        SettingsField.MONITORED_APIS -> localized(language, "APIs monitoradas", "Monitored APIs")
        SettingsField.API_KEY -> localized(language, "chave de API", "API key")
        SettingsField.ANTHROPIC_PROFILES -> localized(language, "contas Anthropic", "Anthropic accounts")
        SettingsField.ANTHROPIC_PROFILE_LABEL -> localized(language, "apelido da conta", "Account label")
        SettingsField.TEAM_INTEGRATION -> localized(language, "integração com time", "Team integration")
        SettingsField.TEAM_SERVER -> localized(language, "servidor do time", "Team server")
        SettingsField.TEAM_KEY -> localized(language, "chave", "Key")
        SettingsField.TEAM_ALIAS -> localized(language, "apelido", "Alias")
        SettingsField.TEAM_ACCOUNTS -> localized(language, "contas do time", "Team accounts")
        SettingsField.TEAM_ADMIN_TOKEN ->
            localized(language, "token de administração", "Admin token")
        SettingsField.NETWORK_PROXY -> localized(language, "proxy", "Proxy")
    }
}
