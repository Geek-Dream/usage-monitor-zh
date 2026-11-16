package com.usagemonitor.presentation.ui

import kotlin.time.Instant
import com.usagemonitor.domain.entity.AppLanguage
import com.usagemonitor.domain.entity.TeamConnectionFailure
import com.usagemonitor.domain.entity.CliSessionHealth
import com.usagemonitor.domain.entity.CliSessionHealthTally
import com.usagemonitor.domain.entity.CliSessionRange

/**
 * Nome da janela.
 *
 * Um time é uma conta Anthropic, então a visão global do administrador mostra
 * vários times e o título vai no plural — no singular ele diria que a janela é
 * de um time só, que é justamente o que ela não é.
 */
internal fun teamUsageTitle(language: AppLanguage, isAdminOverview: Boolean = false): String {
    if (isAdminOverview) {
        return localized(language, "Sessões dos times", "All team sessions")
    }
    return localized(language, "Sessões do time", "Team sessions")
}

/**
 * Título da janela nomeando a conta.
 *
 * [isAdminOverview] vem de quem abriu a janela e não é inferido de
 * [accountLabel] em branco: rótulo vazio numa conta só cairia no mesmo ramo e
 * daria o plural para um time só.
 *
 * [teamName] só vem preenchido quando o painel tem dois ou mais times ativos
 * (o seletor aparece): aí o título diz qual time está aberto, antes da conta.
 */
internal fun teamUsageWindowTitle(
    language: AppLanguage,
    accountLabel: String?,
    isAdminOverview: Boolean = false,
    teamName: String? = null
): String {
    val base = teamUsageTitle(language, isAdminOverview)
    val details = listOfNotNull(teamName, accountLabel).filter { detail -> detail.isNotBlank() }
    if (details.isEmpty()) {
        return base
    }
    return "$base — ${details.joinToString(" · ")}"
}

/**
 * Rótulos da tela de time.
 *
 * O que já existe em [CliSessionsLabels] é reaproveitado — janelas, pílula "ao
 * vivo", carimbo de última alteração e as colunas da linha de sessão são os
 * mesmos das duas telas, e traduzir duas vezes acabaria em textos divergentes.
 */
internal object TeamUsageLabels {

    fun tabMembers(language: AppLanguage): String {
        return localized(language, "Integrantes", "Members")
    }

    fun tabTrend(language: AppLanguage): String {
        return localized(language, "Tendência", "Trend")
    }

    /**
     * O que a tendência **é**, e não só como lê-la.
     *
     * Sem esta frase o painel entrega barras roxas sem dizer que grandeza elas
     * medem, em que recorte, nem por que não obedecem ao filtro de janela logo
     * acima delas.
     */
    fun trendHint(dayCount: Int, language: AppLanguage): String {
        return localized(language, "Quanto cada integrante gastou por dia nos últimos $dayCount dias, uma barra por dia. " +
                "Serve para ver quem subiu ou parou ao longo do tempo — é história, não a janela de quota atual.", "How much each member spent per day over the last $dayCount days, one bar per day. " +
                "It answers who ramped up or stopped over time — it is history, not the current quota window.")
    }

    fun trendEmpty(language: AppLanguage): String {
        return localized(language, "Nenhum consumo registrado nos dias cobertos pela tendência.", "No usage recorded in the days the trend covers.")
    }

    fun trendUnavailable(language: AppLanguage): String {
        return localized(language, "Tendência indisponível: este servidor de time é anterior à rota que a serve.", "Trend unavailable: this team server predates the route that serves it.")
    }

    /**
     * O resumo descreve os **mesmos** turnos da lista, por outros eixos. Sem
     * dizê-lo, os dois totais parecem duas medidas concorrentes.
     */
    fun breakdownHint(language: AppLanguage): String {
        return localized(language, "Os mesmos turnos da lista de integrantes, recortados por eixo. Custo estimado a preço de tabela, calculado nesta máquina.", "The same turns from the member list, sliced by axis. Estimated cost at list price, computed on this machine.")
    }

    // `trendTitle` saiu: era o título de um painel de explicação acima do
    // gráfico, e o painel deixou de existir. A aba já se chama "Tendência" e o
    // `trendHint` já diz quantos dias a série cobre.

    /**
     * A tendência não segue o filtro de janela da lista, e a tela precisa
     * dizê-lo: um gráfico de dias ao lado de números de 5h seria lido como o
     * mesmo recorte.
     */
    fun trendNotice(language: AppLanguage): String {
        return localized(language, "Custo estimado por dia (BRT), independente do filtro de janela acima. Barras na mesma escala para comparar integrantes.", "Estimated cost per day (BRT), independent of the window filter above. Bars share one scale so members are comparable.")
    }


    fun memberCount(count: Int, language: AppLanguage): String {
        return localized(
            language,
            if (count == 1) "1 integrante" else "$count integrantes",
            if (count == 1) "1 member" else "$count members"
        )
    }

    fun sessionCount(count: Int, language: AppLanguage): String {
        return CliSessionsLabels.sessionCount(count, language)
    }

    /** Cabeçalho da coluna de integrantes no totalizador da conta. */
    fun columnMembers(language: AppLanguage): String {
        return localized(language, "Integrantes", "Members")
    }

    fun allAccounts(count: Int, language: AppLanguage): String {
        return localized(
            language,
            if (count == 1) "Todas as contas (1)" else "Todas as contas ($count)",
            if (count == 1) "All accounts (1)" else "All accounts ($count)"
        )
    }

    fun allEmailGroups(emailCount: Int, accountCount: Int, language: AppLanguage): String {
        return localized(language, "$emailCount grupos por e-mail · $accountCount UUIDs", "$emailCount email groups · $accountCount UUIDs")
    }

    fun technicalAccounts(count: Int, language: AppLanguage): String {
        return localized(
            language,
            if (count == 1) "1 UUID" else "$count UUIDs",
            if (count == 1) "1 UUID" else "$count UUIDs"
        )
    }

    /**
     * Rótulo da faixa que abre uma conta na lista global.
     *
     * A faixa mostrava um e-mail e um uuid sem dizer o que eles são, e ao lado de
     * uma linha de integrante — que também tem nome e identificador — as duas
     * liam igual. A palavra é o que separa os dois níveis antes da cor e do recuo.
     */
    fun accountBand(language: AppLanguage): String {
        return localized(language, "Conta", "Account")
    }

    /**
     * A mesma palavra com a contagem de integrantes emendada.
     *
     * A contagem morava numa coluna própria da faixa, que a lista de integrantes
     * não tem. Com a faixa reservando as mesmas colunas da linha, um número sob a
     * legenda "Tempo ativo" diria uma coisa e valeria outra — então ele voltou
     * para onde a faixa se identifica.
     */
    fun accountBandWithMembers(memberCount: Int, language: AppLanguage): String {
        return "${accountBand(language)} · ${memberCount(memberCount, language)}"
    }

    /** Legenda da coluna de identidade, a mesma da tela de presença. */
    fun columnMember(language: AppLanguage): String {
        return TeamPresenceLabels.columnMember(language)
    }

    /** Conta que ainda não tem chave emitida — só o uuid a identifica. */
    fun unlabeledAccount(language: AppLanguage): String {
        return localized(language, "Conta sem chave", "Account without key")
    }

    /**
     * Aviso do recorte de 5h na visão global.
     *
     * Sem ele o número diverge do que cada pessoa vê no próprio modal, e a
     * diferença parece defeito: lá a janela começa no reset de quota da conta,
     * aqui ela não pode começar no reset de nenhuma, porque são várias.
     */
    fun slidingWindowNotice(language: AppLanguage): String {
        return localized(language, "Nesta visão o recorte de 5h é deslizante: cada conta reseta a quota " +
                "numa hora diferente, então os números podem não bater com o modal de uma conta.", "In this view the 5h range is sliding: each account resets its quota at a " +
                "different time, so numbers may differ from a single-account modal.")
    }

    fun columnShare(language: AppLanguage): String {
        return localized(language, "do time", "of team")
    }

    fun columnMachine(language: AppLanguage): String {
        return CliSessionsLabels.machine(language)
    }

    fun columnTokens(language: AppLanguage): String {
        return CliSessionsLabels.columnTokens(language)
    }

    fun columnCost(language: AppLanguage): String {
        return CliSessionsLabels.columnCost(language)
    }

    /** Mesmo rótulo da tela da máquina: é a mesma medida, com o mesmo corte. */
    fun columnActiveTime(language: AppLanguage): String {
        return CliSessionsLabels.activeTime(language)
    }

    fun columnStatus(language: AppLanguage): String {
        return CliSessionsLabels.columnStatus(language)
    }

    fun healthShort(health: CliSessionHealth, language: AppLanguage): String {
        return CliSessionsLabels.healthShort(health, language)
    }

    fun back(language: AppLanguage): String {
        return CliSessionsLabels.back(language)
    }

    fun detailLoading(language: AppLanguage): String {
        return localized(language, "Consultando a sessão no servidor do time…", "Querying the session on the team server…")
    }

    /**
     * Servidor sem a rota de detalhe, ou sessão fora da retenção dele.
     *
     * Diz o que falta e o que fazer: o app não quebra contra um servidor antigo,
     * mas quem lê a tela precisa saber por que os gráficos não estão lá.
     */
    fun missingTurnsNotice(language: AppLanguage): String {
        return localized(language, "Este servidor de time não devolve os turnos desta sessão (versão anterior a 0.2.0 " +
                "ou sessão já expirada na retenção). Só os agregados do período estão disponíveis; " +
                "os gráficos por turno voltam depois de atualizar o servidor.", "This team server does not return the turns for this session (older than 0.2.0 or " +
                "the session already fell out of retention). Only the period aggregates are " +
                "available; the per-turn charts come back once the server is updated.")
    }

    fun rangeLabel(range: CliSessionRange, language: AppLanguage): String {
        return CliSessionsLabels.rangeLabel(range, language)
    }

    fun live(language: AppLanguage): String {
        return CliSessionsLabels.live(language)
    }

    fun lastChange(instantLabel: String?, language: AppLanguage): String {
        return CliSessionsLabels.lastChange(instantLabel, language)
    }

    fun estimatedTotalInRange(
        range: CliSessionRange,
        endsAt: Instant?,
        isAnchored: Boolean,
        language: AppLanguage
    ): String {
        if (range == CliSessionRange.ALL) {
            return CliSessionsLabels.estimatedTotal(language)
        }
        return CliSessionsLabels.estimatedTotalInRange(range, endsAt, isAnchored, language)
    }

    /** Integrante conhecido pelo servidor, mas sem nenhum turno na janela. */
    fun noActivityInRange(language: AppLanguage): String {
        return localized(language, "sem uso no período", "no usage in range")
    }

    /**
     * Nenhum integrante consumiu na janela.
     *
     * Diferente de "ninguém configurou a integração": o time pode existir e
     * simplesmente não ter usado o CLI nas últimas horas.
     */
    fun emptyInRange(range: CliSessionRange, isAnchored: Boolean, language: AppLanguage): String {
        if (range == CliSessionRange.ALL) {
            return localized(language, "Nenhum integrante do time enviou dados ainda. Confira o servidor nas Configurações.", "No team member has reported data yet. Check the server in Settings.")
        }
        val window = rangeLabel(range, language)
        if (isAnchored) {
            return localized(language, "Nenhum uso do time nesta janela de quota ($window). Escolha uma janela maior.", "No team usage in the current quota window ($window). Pick a wider range.")
        }
        return localized(language, "Nenhum uso do time no período ($window). Escolha uma janela maior.", "No team usage in the last $window. Pick a wider range.")
    }

    /** Prefixo do aviso de falha, para o usuário saber que o dado está velho. */
    fun serverError(message: String, language: AppLanguage): String {
        return localized(language, "Não foi possível falar com o servidor de time: $message", "Could not reach the team server: $message")
    }

    fun expand(language: AppLanguage): String {
        return localized(language, "Ver sessões", "Show sessions")
    }

    fun collapse(language: AppLanguage): String {
        return localized(language, "Ocultar sessões", "Hide sessions")
    }

    fun expandAccount(language: AppLanguage): String {
        return localized(language, "Ver integrantes", "Show members")
    }

    fun collapseAccount(language: AppLanguage): String {
        return localized(language, "Ocultar integrantes", "Hide members")
    }

    fun removeMember(language: AppLanguage): String {
        return localized(language, "Remover do time", "Remove from team")
    }

    fun removeSession(language: AppLanguage): String {
        return localized(language, "Excluir sessão", "Delete session")
    }

    fun removeSessionTitle(language: AppLanguage): String {
        return localized(language, "Excluir sessão?", "Delete session?")
    }

    fun removeSessionWarning(
        sessionId: String,
        projectName: String?,
        language: AppLanguage
    ): String {
        val identity = if (projectName.isNullOrBlank()) {
            shortSessionId(sessionId)
        } else {
            "${shortSessionId(sessionId)} ($projectName)"
        }
        return localized(language, "A sessão $identity e todo o consumo já enviado por ela serão apagados do " +
                "servidor. O histórico antigo não será reenviado e isto não tem volta. " +
                "Se a sessão continuar ativa, novos turnos poderão recriá-la.", "Session $identity and all usage already reported by it will be deleted from " +
                "the server. The old history will not be sent again and this cannot be " +
                "undone. If the session remains active, new turns may recreate it.")
    }

    fun confirmSessionRemoval(language: AppLanguage): String {
        return localized(language, "Excluir", "Delete")
    }

    fun removeMemberTitle(language: AppLanguage): String {
        return localized(language, "Remover integrante?", "Remove member?")
    }

    /**
     * Texto da confirmação, com o custo real da ação.
     *
     * A remoção apaga os turnos no servidor, e a máquina de origem não os
     * reenvia — o marcador local dela já os considera entregues. Omitir isso
     * transformaria um botão de limpeza em perda silenciosa de histórico.
     */
    fun removeMemberWarning(alias: String, language: AppLanguage): String {
        return localized(language, "\"$alias\" e todo o consumo já enviado por essa máquina serão apagados do " +
                "servidor. A máquina de origem não reenvia o histórico: isto não tem volta. " +
                "Use apenas para tirar da lista uma máquina que não existe mais.", "\"$alias\" and all usage already reported by that machine will be deleted from " +
                "the server. The source machine does not resend its history: this cannot be " +
                "undone. Use it only to drop a machine that no longer exists.")
    }

    fun confirmRemoval(language: AppLanguage): String {
        return localized(language, "Remover", "Remove")
    }

    fun cancel(language: AppLanguage): String {
        return CommonLabels.cancel(language)
    }

    /** Esta máquina não pode se remover: o próximo envio a recriaria. */
    fun cannotRemoveSelf(language: AppLanguage): String {
        return localized(language, "esta máquina", "this machine")
    }

    /**
     * Remoção só existe na visão global, então a credencial é o token admin. Com a
     * etapa classificada (#494) ela entra no lugar da mensagem crua.
     */
    fun removalError(message: String, language: AppLanguage, failure: TeamConnectionFailure? = null): String {
        val reason = actionReason(message, language, failure)
        return localized(language, "Não foi possível remover o integrante: $reason", "Could not remove the member: $reason")
    }

    fun sessionRemovalError(message: String, language: AppLanguage, failure: TeamConnectionFailure? = null): String {
        val reason = actionReason(message, language, failure)
        return localized(language, "Não foi possível excluir a sessão: $reason", "Could not delete the session: $reason")
    }

    private fun actionReason(message: String, language: AppLanguage, failure: TeamConnectionFailure?): String {
        if (failure == null) {
            return message
        }
        return TeamConnectionLabels.headline(failure, TeamCredentialKind.ADMIN_TOKEN, language)
    }

    /** Mesma contagem do modal local: "1 saturada · 2 em atenção". */
    fun healthTally(tally: CliSessionHealthTally, language: AppLanguage): String? {
        return CliSessionsLabels.healthTally(tally, language)
    }

    /** Deixa explícito de onde vem o número, já que a fonte não é esta máquina. */
    fun lastSeen(instantLabel: String?, language: AppLanguage): String {
        if (instantLabel == null) {
            return localized(language, "nunca reportou", "never reported")
        }
        // "envio", e não "último envio": a frase mora na terceira linha da coluna
        // de identidade, e as duas palavras a faziam truncar justamente no
        // carimbo, que é a parte que informa.
        return localized(language, "envio $instantLabel", "report $instantLabel")
    }
}
