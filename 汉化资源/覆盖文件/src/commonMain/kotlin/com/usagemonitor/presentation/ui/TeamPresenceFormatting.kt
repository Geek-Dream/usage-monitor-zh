package com.usagemonitor.presentation.ui

import com.usagemonitor.domain.entity.AppLanguage
import com.usagemonitor.domain.entity.TeamConnectionFailure

/** Nome da janela de presença. */
internal fun teamPresenceTitle(language: AppLanguage, isAdminOverview: Boolean = false): String {
    if (isAdminOverview) {
        return localized(language, "Conectados agora — todas as contas", "Connected now — all accounts")
    }
    return localized(language, "Conectados agora", "Connected now")
}

/**
 * Título da janela nomeando a conta.
 *
 * [isAdminOverview] vem de quem abriu a janela e não é inferido de
 * [accountLabel] em branco, pelo mesmo motivo de [teamUsageWindowTitle]: rótulo
 * vazio numa conta só cairia no ramo global.
 */
internal fun teamPresenceWindowTitle(
    language: AppLanguage,
    accountLabel: String?,
    isAdminOverview: Boolean = false
): String {
    val base = teamPresenceTitle(language, isAdminOverview)
    if (accountLabel.isNullOrBlank()) {
        return base
    }
    return "$base — $accountLabel"
}

/**
 * Rótulos da tela de presença.
 *
 * Reaproveita [CliSessionsLabels] e [TeamUsageLabels] onde o texto já existe —
 * máquina, status, pílula "ao vivo" e carimbo de última alteração são os mesmos
 * das outras telas, e traduzi-los de novo acabaria em textos divergentes.
 */
internal object TeamPresenceLabels {

    fun online(language: AppLanguage): String {
        return localized(language, "Conectado", "Connected")
    }

    fun offline(language: AppLanguage): String {
        return localized(language, "Desconectado", "Disconnected")
    }

    /** Legenda da coluna de identidade, na faixa de cabeçalho da lista. */
    fun columnMember(language: AppLanguage): String {
        return localized(language, "Integrante", "Member")
    }

    fun columnState(language: AppLanguage): String {
        return localized(language, "Estado", "State")
    }

    /**
     * O terceiro estado da coluna Estado.
     *
     * Era a legenda de uma coluna própria — "Trabalhando agora" — que carregava a
     * contagem de sessões e o carimbo do último turno. As duas viraram colunas, e
     * a palavra virou o valor que o estado assume quando há turno nos últimos
     * cinco minutos.
     */
    fun workingNow(language: AppLanguage): String {
        return localized(language, "Trabalhando agora", "Working now")
    }

    fun columnActiveSessions(language: AppLanguage): String {
        return localized(language, "Sessões ativas", "Active sessions")
    }

    fun columnLastSeen(language: AppLanguage): String {
        return localized(language, "Último sinal", "Last signal")
    }

    fun columnLastTurn(language: AppLanguage): String {
        return localized(language, "Último turno", "Last turn")
    }

    /** Célula sem carimbo: a máquina nunca reportou nada. */
    fun neverSeen(language: AppLanguage): String = "—"

    fun onlyOnline(language: AppLanguage): String {
        return localized(language, "Somente conectados", "Connected only")
    }

    fun filterPlaceholder(language: AppLanguage): String {
        return localized(language, "Filtrar integrante", "Filter member")
    }

    /** Nenhuma linha sobreviveu ao texto digitado, e o time não está vazio. */
    fun emptyQuery(query: String, language: AppLanguage): String {
        return localized(language, "Ninguém com \"$query\" no apelido ou na máquina.", "Nobody matching \"$query\" in alias or machine.")
    }

    fun thisMachine(language: AppLanguage): String {
        return localized(language, "esta máquina", "this machine")
    }

    fun lastSignal(instantLabel: String?, language: AppLanguage): String {
        if (instantLabel == null) {
            return neverReported(language)
        }
        return localized(language, "último sinal $instantLabel", "last signal $instantLabel")
    }

    fun lastTurn(instantLabel: String?, language: AppLanguage): String {
        if (instantLabel == null) {
            return localized(language, "sem turno recente", "no recent turn")
        }
        return localized(language, "último turno $instantLabel", "last turn $instantLabel")
    }

    fun neverReported(language: AppLanguage): String {
        return localized(language, "nunca reportou", "never reported")
    }

    /** Resumo do cabeçalho: quantos trabalham, quantos estão conectados, quantos há. */
    fun workingSummary(count: Int, language: AppLanguage): String {
        return localized(
            language,
            if (count == 1) "1 trabalhando" else "$count trabalhando",
            if (count == 1) "1 working" else "$count working"
        )
    }

    fun onlineSummary(count: Int, language: AppLanguage): String {
        return localized(
            language,
            if (count == 1) "1 conectado" else "$count conectados",
            if (count == 1) "1 connected" else "$count connected"
        )
    }

    fun knownSummary(count: Int, language: AppLanguage): String {
        return localized(
            language,
            if (count == 1) "1 conhecido" else "$count conhecidos",
            if (count == 1) "1 known" else "$count known"
        )
    }

    /** Quantos da conta estão conectados, para a faixa da visão global. */
    fun groupSummary(online: Int, total: Int, language: AppLanguage): String {
        return localized(language, "$online de $total conectados", "$online of $total connected")
    }

    /**
     * Os dois agregados da conta numa frase só, para a faixa da visão global.
     *
     * Juntos e não em células separadas porque a faixa reserva as mesmas colunas
     * da linha, e nenhuma delas tem por valor "1 trabalhando": um número cru sob
     * a legenda "Último turno" diria uma coisa e valeria outra. A frase se
     * descreve e por isso pode atravessar as três colunas que a conta não tem.
     */
    fun accountBandSummary(
        online: Int,
        total: Int,
        working: Int,
        language: AppLanguage
    ): String {
        return "${groupSummary(online, total, language)} · ${workingSummary(working, language)}"
    }

    fun empty(language: AppLanguage): String {
        return localized(language, "Ninguém apareceu ainda. Cada máquina reporta ao abrir o app.", "Nobody has shown up yet. Each machine reports when the app opens.")
    }

    fun emptyFiltered(language: AppLanguage): String {
        return localized(language, "Ninguém conectado agora. Desligue o filtro para ver o time inteiro.", "Nobody connected right now. Turn off the filter to see the whole team.")
    }

    fun error(message: String, language: AppLanguage): String {
        return localized(language, "Não foi possível ler a presença do time: $message", "Could not read team presence: $message")
    }

    fun deleteAccount(language: AppLanguage): String {
        return localized(language, "Apagar conta do servidor", "Delete account data")
    }

    fun deleteAccountTitle(language: AppLanguage): String {
        return localized(language, "Apagar a conta inteira?", "Delete whole account?")
    }

    /**
     * Texto da confirmação, com o custo real e com a única trava que existe.
     *
     * Apagar não impede a conta de voltar: envio e presença reivindicam sozinhos,
     * então uma máquina que ainda participe dela a recria na batida seguinte.
     * Omitir isso faria o administrador achar que a limpeza falhou.
     */
    fun deleteAccountWarning(
        accountKey: String,
        memberCount: Int,
        language: AppLanguage
    ): String {
        return localized(language, "A conta $accountKey, os $memberCount integrante(s) dela e todo o consumo já " +
                "enviado serão apagados do servidor. Isto não tem volta. Se alguma máquina " +
                "ainda participar dessa conta, ela reaparece no próximo sinal — desmarque a " +
                "conta lá, ou reduza o limite de contas da chave.", "Account $accountKey, its $memberCount member(s) and all usage already reported " +
                "will be deleted from the server. This cannot be undone. If any machine still " +
                "reports for this account it will come back on the next signal — unselect the " +
                "account there, or lower the key's account limit.")
    }

    fun confirmAccountDeletion(language: AppLanguage): String {
        return localized(language, "Apagar", "Delete")
    }

    /** A própria conta não pode ser apagada: esta máquina a recriaria. */
    fun cannotDeleteOwnAccount(language: AppLanguage): String {
        return localized(language, "esta máquina participa desta conta", "this machine reports for this account")
    }

    /** Ação só existe na visão global: a credencial é o token admin (#494). */
    fun actionError(message: String, language: AppLanguage, failure: TeamConnectionFailure? = null): String {
        val reason = if (failure == null) {
            message
        } else {
            TeamConnectionLabels.headline(failure, TeamCredentialKind.ADMIN_TOKEN, language)
        }
        return localized(language, "Não foi possível concluir a remoção: $reason", "Could not complete the removal: $reason")
    }

    /**
     * Aviso de relógios divergentes.
     *
     * A tela avisa em vez de mostrar um número errado com cara de certo: o
     * carimbo de presença vem do relógio do servidor, e sem NTP nos dois lados a
     * coluna de estado deixa de significar o que promete.
     */
    fun clockSkewNotice(minutes: Long, language: AppLanguage): String {
        return localized(language, "Os relógios desta máquina e do servidor divergem em cerca de " +
                "$minutes min. A coluna Estado pode estar errada.", "This machine's clock and the server's differ by about " +
                "$minutes min. The State column may be wrong.")
    }
}
