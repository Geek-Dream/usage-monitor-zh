package com.usagemonitor.presentation.ui

import com.usagemonitor.domain.entity.AppLanguage

/**
 * Rótulos da administração de chaves do time. Saíram de `TeamKeysAdminScreen.kt` (747 linhas)
 * pelo padrão de `TeamUsageFormatting.kt` e `TeamPresenceFormatting.kt`: tela num arquivo,
 * texto no outro.
 */
internal object TeamKeysLabels {

    fun loading(language: AppLanguage): String = localized(language, "Consultando as chaves no servidor…", "Querying keys on the server…")

    fun explanation(language: AppLanguage): String = localized(language, "O rótulo é a relação do time daquela chave: escreva ali o e-mail da pessoa, e só a " +
            "conta com aquele e-mail poderá usá-la. Separe por vírgula para incluir mais de uma. " +
            "Rótulo sem e-mail aceita qualquer conta.", "The label is that key's team roster: type the person's e-mail there and only the " +
            "account with that e-mail may use it. Separate with commas to list more than one. " +
            "A label without an e-mail accepts any account.")

    fun empty(language: AppLanguage): String = localized(language, "Nenhuma chave emitida ainda.", "No keys issued yet.")

    fun labelField(language: AppLanguage): String =
        localized(language, "Rótulo", "Label")

    fun labelPlaceholder(language: AppLanguage): String =
        localized(language, "fulano@empresa.com", "person@company.com")

    fun maxAccounts(language: AppLanguage): String =
        localized(language, "contas", "accounts")

    fun create(language: AppLanguage): String =
        localized(language, "Emitir", "Issue")

    fun show(language: AppLanguage): String =
        localized(language, "Mostrar", "Show")

    fun hide(language: AppLanguage): String =
        localized(language, "Ocultar", "Hide")

    fun copy(language: AppLanguage): String =
        CommonLabels.copy(language)

    fun unclaimed(language: AppLanguage): String = localized(language, "Sem conta vinculada — o vínculo nasce no primeiro envio.", "No account linked yet — the link is created on the first upload.")

    fun unlink(language: AppLanguage): String =
        localized(language, "Desvincular", "Unlink")

    fun regenerate(language: AppLanguage): String =
        localized(language, "Regerar", "Regenerate")

    fun revoke(language: AppLanguage): String =
        localized(language, "Revogar", "Revoke")

    fun revoked(language: AppLanguage): String =
        localized(language, "revogada", "revoked")

    fun rename(language: AppLanguage): String =
        localized(language, "Renomear", "Rename")

    fun save(language: AppLanguage): String =
        localized(language, "Salvar", "Save")

    fun cancel(language: AppLanguage): String =
        CommonLabels.cancel(language)

    fun dismiss(language: AppLanguage): String =
        localized(language, "Dispensar", "Dismiss")

    fun retry(language: AppLanguage): String =
        localized(language, "Tentar de novo", "Retry")

    fun noEmail(language: AppLanguage): String =
        localized(language, "sem e-mail reportado", "no reported e-mail")

    fun unauthorizedAccount(language: AppLanguage): String =
        localized(language, "Fora do rótulo", "Outside the label")

    fun removeAccount(language: AppLanguage): String =
        localized(language, "Remover do time", "Remove from team")

    fun removeAccountTitle(language: AppLanguage): String = localized(language, "Remover esta conta do time?", "Remove this account from the team?")

    /**
     * Diz as duas coisas que a ação faz, porque a segunda é irreversível e a
     * primeira, sozinha, seria desfeita pelo envio seguinte daquela máquina.
     */
    fun removeAccountWarning(account: String, language: AppLanguage): String =
        localized(language, "Apaga tudo o que $account enviou — integrantes, sessões e turnos — e passa a " +
                "recusá-la, mesmo que a máquina dela continue enviando. Os dados não voltam nem " +
                "depois de devolvê-la ao time.", "Deletes everything $account sent — members, sessions and turns — and starts " +
                "refusing it, even if that machine keeps uploading. The data does not come back, " +
                "not even after returning the account to the team.")

    fun confirmRemoveAccount(language: AppLanguage): String =
        localized(language, "Remover do time", "Remove from team")

    fun blockedSection(language: AppLanguage): String =
        localized(language, "Contas fora do time", "Accounts outside the team")

    fun blockedEmpty(language: AppLanguage): String = localized(language, "Nenhuma conta removida.", "No account removed.")

    fun unblock(language: AppLanguage): String =
        localized(language, "Devolver ao time", "Return to team")

    fun blockedAt(timestamp: String, language: AppLanguage): String =
        localized(language, "removida em $timestamp", "removed on $timestamp")
}
