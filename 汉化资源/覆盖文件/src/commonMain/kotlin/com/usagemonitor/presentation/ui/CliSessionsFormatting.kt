package com.usagemonitor.presentation.ui

import com.usagemonitor.domain.entity.OutputThroughput

import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.time.Instant
import com.usagemonitor.domain.entity.AppLanguage
import com.usagemonitor.domain.entity.CliSessionHealth
import com.usagemonitor.domain.entity.CliSessionHealthTally
import com.usagemonitor.domain.entity.CliSessionRange
import com.usagemonitor.domain.entity.MICROS_PER_USD

/** Formata micros de USD como `$1.2345` — quatro casas, porque turnos custam centavos de centavo. */
internal fun formatMicrosUsd(micros: Long): String {
    val sign = if (micros < 0L) "-" else ""
    val absolute = abs(micros)
    val dollars = absolute / MICROS_PER_USD
    val fraction = (absolute % MICROS_PER_USD) / 100L
    return "$sign\$$dollars.${fraction.toString().padStart(4, '0')}"
}

/** Versão curta para totais de lista: `$12.34`. */
internal fun formatMicrosUsdShort(micros: Long): String {
    val sign = if (micros < 0L) "-" else ""
    val absolute = abs(micros)
    val dollars = absolute / MICROS_PER_USD
    val cents = (absolute % MICROS_PER_USD) / 10_000L
    return "$sign\$$dollars.${cents.toString().padStart(2, '0')}"
}

internal fun formatPercent(fraction: Double): String {
    return "${(fraction * 100.0).roundToLong()}%"
}

internal fun shortSessionId(sessionId: String): String {
    return sessionId.take(8)
}

/**
 * Comando que retoma a sessão no Claude Code.
 *
 * Leva o id **inteiro**: o `--resume` só retoma direto quando recebe o session ID
 * completo — com um prefixo ele cai no seletor interativo usando o texto como
 * busca. Os oito caracteres que a tela mostra ([shortSessionId]) não servem para
 * isso, e é justamente por isso que existe o botão de copiar.
 */
internal fun resumeSessionCommand(sessionId: String): String {
    return "claude --resume $sessionId"
}

/** Comando que retoma uma sessão do Codex CLI. */
internal fun codexResumeSessionCommand(sessionId: String): String {
    return "codex resume $sessionId"
}

internal fun cliSessionsTitle(language: AppLanguage): String {
    return localized(language, "Sessões CLI", "CLI Sessions")
}

/** Título da janela nomeando a conta, para não confundir sessões de perfis diferentes. */
internal fun cliSessionsWindowTitle(language: AppLanguage, profileLabel: String?): String {
    val base = cliSessionsTitle(language)
    if (profileLabel.isNullOrBlank()) {
        return base
    }
    return "$base — $profileLabel"
}

/** Título da janela de sessões do Codex CLI, nomeando a conta lida (ex.: "Padrão"). */
internal fun codexCliSessionsWindowTitle(language: AppLanguage, profileLabel: String): String {
    val base = localized(language, "Sessões Codex CLI", "Codex CLI Sessions")
    if (profileLabel.isBlank()) {
        return base
    }
    return "$base — $profileLabel"
}

internal object CliSessionsLabels {

    fun sessionCount(count: Int, language: AppLanguage): String {
        return localized(
            language,
            if (count == 1) "1 sessão" else "$count sessões",
            if (count == 1) "1 session" else "$count sessions"
        )
    }

    fun estimatedTotal(language: AppLanguage): String {
        return localized(language, "custo estimado", "estimated cost")
    }

    /**
     * Deixa explícito que os totais do header são só do período selecionado — e,
     * quando o corte está ancorado na quota, até quando essa janela vale. Sem o
     * horário, "5h" seria confundido com as últimas cinco horas corridas.
     *
     * Ancorado sem [endsAt] é a janela recém-aberta: o reset venceu, o corte já é
     * o dele, mas o fim novo só existe quando a API o publicar. Dizer "até
     * HH:MM" ali seria inventar um horário.
     */
    fun estimatedTotalInRange(
        range: CliSessionRange,
        endsAt: Instant?,
        isAnchored: Boolean,
        language: AppLanguage
    ): String {
        val window = rangeLabel(range, language)
        if (endsAt == null && isAnchored) {
            return localized(language, "custo estimado · janela $window desde o reinício", "estimated cost · $window window since the reset")
        }
        if (endsAt == null) {
            return "${estimatedTotal(language)} · ${lastRangeLabel(range, language)}"
        }
        return localized(language, "custo estimado · janela $window até ${formatInstant(endsAt)}", "estimated cost · $window window until ${formatInstant(endsAt)}")
    }

    /**
     * Janela corrida por extenso: "últimas 5h", "últimos 7 dias".
     *
     * "5h" concorda no feminino (horas) e "7 dias"/"30 dias" no masculino — sem a
     * flexão o rótulo saía como "últimas 7 dias".
     */
    private fun lastRangeLabel(range: CliSessionRange, language: AppLanguage): String {
        val window = rangeLabel(range, language)
        return localized(
            language,
            if (range == CliSessionRange.LAST_5H) "últimas $window" else "últimos $window",
            "last $window"
        )
    }

    /** Mesma flexão de [lastRangeLabel], já contraída com a preposição. */
    private fun inLastRangeLabel(range: CliSessionRange, language: AppLanguage): String {
        val window = rangeLabel(range, language)
        return localized(
            language,
            if (range == CliSessionRange.LAST_5H) "nas últimas $window" else "nos últimos $window",
            "in the last $window"
        )
    }

    fun rangeLabel(range: CliSessionRange, language: AppLanguage): String {
        return when (range) {
            CliSessionRange.LAST_5H -> "5h"
            CliSessionRange.LAST_7D -> localized(language, "7 dias", "7 days")
            CliSessionRange.LAST_30D -> localized(language, "30 dias", "30 days")
            CliSessionRange.ALL -> localized(language, "Total", "All time")
        }
    }

    /** A tela se atualiza sozinha; a pílula existe para o usuário saber disso. */
    fun live(language: AppLanguage): String {
        return localized(language, "AO VIVO", "LIVE")
    }

    /**
     * Marca a última **mudança**, não a última varredura. Se nada mudou o carimbo
     * não anda — e é exatamente isso que aconteceu.
     */
    fun lastChange(instantLabel: String?, language: AppLanguage): String {
        if (instantLabel == null) {
            return localized(language, "sem alterações ainda", "no changes yet")
        }
        return localized(language, "última alteração $instantLabel", "last change $instantLabel")
    }

    fun back(language: AppLanguage): String {
        return CommonLabels.back(language)
    }

    fun copyResumeCommand(language: AppLanguage): String {
        return localized(language, "Copiar comando de retomada", "Copy resume command")
    }

    fun copied(language: AppLanguage): String {
        return localized(language, "Copiado", "Copied")
    }

    fun empty(language: AppLanguage): String {
        return localized(language, "Nenhuma sessão do Claude Code encontrada em ~/.claude/projects.", "No Claude Code sessions found in ~/.claude/projects.")
    }

    /**
     * Lista vazia com filtro ativo não significa índice vazio — dizer que não há
     * nada em `~/.claude/projects` seria falso na maioria das vezes.
     */
    fun emptyInRange(range: CliSessionRange, isAnchored: Boolean, language: AppLanguage): String {
        if (range == CliSessionRange.ALL) {
            return empty(language)
        }
        val window = rangeLabel(range, language)
        // Vale tanto para a janela em curso quanto para a que acabou de abrir no
        // reset: nos dois casos o vazio é da janela de quota, não do índice.
        if (isAnchored) {
            return localized(language, "Nenhuma sessão nesta janela de quota ($window). Escolha uma janela maior.", "No session in the current quota window ($window). Pick a wider range.")
        }
        return localized(language, "Nenhuma sessão com atividade ${inLastRangeLabel(range, language)}. Escolha uma janela maior.", "No session active ${inLastRangeLabel(range, language)}. Pick a wider range.")
    }

    fun machine(language: AppLanguage): String {
        return localized(language, "Máquina", "Machine")
    }

    fun projectPath(language: AppLanguage): String {
        return localized(language, "Projeto", "Project")
    }

    fun branch(language: AppLanguage): String {
        return localized(language, "Branch", "Branch")
    }

    fun period(language: AppLanguage): String {
        return localized(language, "Período", "Period")
    }

    fun loading(language: AppLanguage): String {
        return localized(language, "Indexando transcripts…", "Indexing transcripts…")
    }

    fun columnSession(language: AppLanguage): String {
        return localized(language, "Sessão", "Session")
    }

    fun columnProject(language: AppLanguage): String {
        return localized(language, "Projeto", "Project")
    }

    fun columnModel(language: AppLanguage): String {
        return localized(language, "Modelo", "Model")
    }

    /** Respostas do modelo num turno do Codex (tela, resumo e PDF). */
    fun responses(language: AppLanguage): String {
        return localized(language, "Respostas", "Responses")
    }

    fun columnWhen(language: AppLanguage): String {
        return localized(language, "Quando", "When")
    }

    /**
     * O total inclui o cache lido, que costuma responder por mais de 95% dele —
     * cada turno relê o contexto inteiro. Sem o qualificador o número é lido como
     * volume de conteúdo produzido, que ele não é.
     */
    /**
     * Rótulo do bloco de total de sessões.
     *
     * Plural e sem número, ao contrário de [sessionCount]: aquele é a célula da
     * lista do time, onde valor e palavra andam juntos ("2 sessões"). Aqui o
     * número é o valor do bloco e a palavra é o rótulo dele.
     */
    fun columnSessions(language: AppLanguage): String {
        return localized(language, "Sessões", "Sessions")
    }

    fun columnTokens(language: AppLanguage): String {
        return localized(language, "Tokens (com cache)", "Tokens (with cache)")
    }

    /** Composição do total, para o número grande poder ser conferido. */
    fun tokensBreakdown(
        inputTokens: Long,
        outputTokens: Long,
        cacheReadTokens: Long,
        cacheWriteTokens: Long,
        language: AppLanguage
    ): String {
        val read = localized(language, "cache lido", "cache read")
        val write = localized(language, "cache gravado", "cache write")
        return "in ${formatQuantity(inputTokens)} · out ${formatQuantity(outputTokens)} · " +
            "$read ${formatQuantity(cacheReadTokens)} · $write ${formatQuantity(cacheWriteTokens)}"
    }

    fun columnCache(language: AppLanguage): String = "Cache"

    /**
     * Nome da coluna da fatia.
     *
     * Não é o `TeamUsageLabels.columnShare`, que é "do time": aquele é o rótulo
     * de uma célula que já traz o número junto ("15% do time"), e como legenda de
     * coluna ele diria a preposição sem o substantivo.
     */
    fun columnShare(language: AppLanguage): String {
        return localized(language, "Participação", "Share")
    }

    fun columnCost(language: AppLanguage): String {
        return localized(language, "Custo", "Cost")
    }

    fun sortDescendingState(language: AppLanguage): String {
        return localized(language, "ordenado do maior para o menor", "sorted from highest to lowest")
    }

    fun sortAscendingState(language: AppLanguage): String {
        return localized(language, "ordenado do menor para o maior", "sorted from lowest to highest")
    }

    fun input(language: AppLanguage): String = "Input"

    fun output(language: AppLanguage): String = "Output"

    fun cacheRead(language: AppLanguage): String {
        return localized(language, "Cache lido", "Cache read")
    }

    fun cacheWrite(language: AppLanguage): String {
        return localized(language, "Cache gravado", "Cache write")
    }

    fun cacheHitRate(language: AppLanguage): String {
        return localized(language, "Taxa de acerto de cache", "Cache hit rate")
    }

    /** Rótulo do bloco de 168dp: "Taxa de acerto de cache" com o `?` cortava (#393). */
    fun cacheHitRateShort(language: AppLanguage): String {
        return localized(language, "Acerto do cache", "Cache hit rate")
    }

    fun outputPerTurnChart(language: AppLanguage): String {
        return localized(language, "Saída por turno", "Output per turn")
    }

    /**
     * Não é a duração da sessão: intervalos acima de cinco minutos entre turnos
     * são descartados por serem o usuário parado, não tempo de trabalho.
     */
    fun activeTime(language: AppLanguage): String {
        return localized(language, "Tempo ativo", "Active time")
    }

    /** Vazão de saída (#381). */
    fun throughput(language: AppLanguage): String {
        return localized(language, "Vazão", "Throughput")
    }

    /** O que a vazão mede: do pedido à última linha, não a decodificação pura. */
    fun throughputFooter(language: AppLanguage): String {
        return localized(language, "saída · ponta a ponta", "output · end to end")
    }

    fun costDistribution(language: AppLanguage): String {
        return localized(language, "Distribuição de custo", "Cost distribution")
    }

    fun savings(language: AppLanguage): String {
        return localized(language, "Economia do cache", "Cache savings")
    }

    fun savingsExplanation(language: AppLanguage): String {
        return localized(language, "O que os tokens lidos do cache custariam como input, menos o que custaram.", "What the cached tokens would have cost as input, minus what they cost.")
    }

    fun averageContext(language: AppLanguage): String {
        return localized(language, "Contexto médio/turno", "Avg context/turn")
    }

    fun liveContext(language: AppLanguage): String {
        return localized(language, "Contexto vivo", "Live context")
    }

    fun nextInteraction(language: AppLanguage): String {
        return localized(language, "Custo da próxima msg", "Next message cost")
    }

    fun saturation(language: AppLanguage): String {
        return localized(language, "Janela de contexto", "Context window")
    }

    fun saturated(language: AppLanguage): String {
        return localized(language, "Sessão saturada", "Session saturated")
    }

    fun healthTitle(health: CliSessionHealth, language: AppLanguage): String {
        return when (health) {
            CliSessionHealth.HEALTHY ->
                localized(language, "Sessão saudável", "Healthy session")
            CliSessionHealth.ATTENTION ->
                localized(language, "Contexto crescendo", "Context growing")
            CliSessionHealth.SATURATED ->
                localized(language, "Sessão saturada", "Session saturated")
        }
    }

    /**
     * A ação correta no Claude Code é `/compact` — o "Retomar do resumo" do
     * legado era o fluxo do Claude Desktop e não existe aqui.
     */
    fun healthAdvice(health: CliSessionHealth, language: AppLanguage): String {
        return when (health) {
            CliSessionHealth.HEALTHY -> localized(language, "Contexto folgado na janela do modelo; pode seguir.", "Plenty of room left in the model window; carry on.")
            CliSessionHealth.ATTENTION -> localized(language, "Cada mensagem já reenvia bastante contexto. Um /compact reduz o custo daqui pra frente.", "Each message now resends a lot of context. A /compact cuts the cost from here on.")
            CliSessionHealth.SATURATED -> localized(language, "Considere /compact ou abrir uma sessão nova: continuar assim sai caro e a janela está perto do limite.", "Consider /compact or a fresh session: carrying on is expensive and the window is near its limit.")
        }
    }

    /** Versão de uma palavra para a linha da lista; o detalhe usa [healthTitle]. */
    fun healthShort(health: CliSessionHealth, language: AppLanguage): String {
        return when (health) {
            CliSessionHealth.HEALTHY ->
                localized(language, "Saudável", "Healthy")
            CliSessionHealth.ATTENTION ->
                localized(language, "Atenção", "Attention")
            CliSessionHealth.SATURATED ->
                localized(language, "Saturada", "Saturated")
        }
    }

    /**
     * Marca de sessão sem resposta.
     *
     * Diz o fato medido — não houve resposta desde o último pedido — e não
     * "travada": o app lê o transcript, não o sistema operacional. Mesma reserva
     * do texto da notificação.
     */
    fun stalledLabel(pendingMillis: Long, language: AppLanguage): String {
        val elapsed = formatActiveTime(pendingMillis)
        return localized(language, "Sem resposta há $elapsed", "No reply for $elapsed")
    }

    fun columnStatus(language: AppLanguage): String {
        return localized(language, "Status", "Status")
    }

    /**
     * "1 saturada · 2 em atenção" para o cabeçalho. `null` quando não há alerta.
     *
     * Nulo, e não string vazia: um alerta que não existe não deve ocupar linha
     * no cabeçalho. Sessão com janela de contexto desconhecida não entra na
     * contagem — quem filtra é [tallyHealth], que se recusa a chutar a fração.
     */
    fun healthTally(tally: CliSessionHealthTally, language: AppLanguage): String? {
        if (!tally.hasWarnings) {
            return null
        }

        val parts = mutableListOf<String>()
        if (tally.saturated > 0) {
            parts += saturatedCount(tally.saturated, language)
        }
        if (tally.attention > 0) {
            parts += attentionCount(tally.attention, language)
        }
        return parts.joinToString(" · ")
    }

    private fun saturatedCount(count: Int, language: AppLanguage): String {
        return localized(
            language,
            if (count == 1) "1 saturada" else "$count saturadas",
            "$count saturated"
        )
    }

    private fun attentionCount(count: Int, language: AppLanguage): String {
        return localized(language, "$count em atenção", "$count needing attention")
    }

    /** Explicita por que o status foi atribuído — sem isso o alerta é opaco. */
    fun healthReason(
        saturationLabel: String?,
        nextCostLabel: String,
        language: AppLanguage
    ): String {
        // Sem a janela do modelo não há fração para mostrar; dizer isso é melhor
        // que emendar o rótulo de erro na frase da fração.
        val window = if (saturationLabel == null) {
            localized(language, "janela do modelo desconhecida", "unknown model window")
        } else {
            localized(language, "$saturationLabel da janela", "$saturationLabel of the window")
        }
        return localized(language, "$window · $nextCostLabel por mensagem", "$window · $nextCostLabel per message")
    }

    /**
     * O bloco recolhido não é "mais do mesmo": é a apuração por trás dos quatro
     * números do resumo. O rótulo evita prometer novidade onde há detalhamento.
     */
    fun advancedToggle(language: AppLanguage): String {
        return localized(language, "Avançado", "Advanced")
    }

    /** Diz o que está lá dentro para que abrir não seja aposta. */
    fun advancedHint(language: AppLanguage): String {
        return localized(language, "composição dos tokens, distribuição do custo e gráficos por turno", "token breakdown, cost distribution and per-turn charts")
    }

    fun glossaryTitle(language: AppLanguage): String {
        return localized(language, "Como ler esta tela", "How to read this screen")
    }

    fun contextPerTurnChart(language: AppLanguage): String {
        return localized(language, "Contexto por turno", "Context per turn")
    }

    fun cacheWritePerTurnChart(language: AppLanguage): String {
        return localized(language, "Cache gravado por turno", "Cache write per turn")
    }

    fun costVersusSavingsChart(language: AppLanguage): String {
        return localized(language, "Custo x economia acumulados", "Cumulative cost vs savings")
    }

    fun chartContextLegend(language: AppLanguage): String {
        return localized(language, "Contexto", "Context")
    }

    fun chartCostLegend(language: AppLanguage): String {
        return localized(language, "Custo", "Cost")
    }

    fun chartSavingsLegend(language: AppLanguage): String {
        return localized(language, "Economia", "Savings")
    }

    fun estimatedCostNotice(language: AppLanguage): String {
        return localized(language, "Custo estimado a preço de tabela. Não é fatura.", "Estimated at list price. Not an invoice.")
    }

    fun unpricedNotice(count: Int, language: AppLanguage): String {
        return localized(language, "$count turno(s) sem preço conhecido: o custo exibido está incompleto.", "$count turn(s) with unknown pricing: the displayed cost is incomplete.")
    }

    fun staleNotice(language: AppLanguage): String {
        return localized(language, "Transcript removido do disco; só o resumo indexado permanece.", "Transcript removed from disk; only the indexed summary remains.")
    }

    fun sidechainNotice(count: Int, language: AppLanguage): String {
        return localized(language, "$count turno(s) de subagente somam no custo mas ficam fora dos gráficos de contexto.", "$count subagent turn(s) count toward cost but stay out of the context charts.")
    }

    fun turnsLabel(count: Int, language: AppLanguage): String {
        return localized(language, "$count turnos", "$count turns")
    }
}

/**
 * Tempo ativo como `1h05` ou `18min`.
 *
 * Segundos ficam de fora: o corte de pausa é de cinco minutos, então precisão
 * abaixo do minuto sugeriria uma exatidão que a medida não tem.
 */
internal fun formatActiveTime(millis: Long): String {
    val minutes = millis / 60_000L
    val hours = minutes / 60L
    if (hours <= 0L) {
        return "${minutes}min"
    }
    return "${hours}h${(minutes % 60L).toString().padStart(2, '0')}"
}

/**
 * Vazão de saída em tokens por segundo, arredondada (#381). "—" quando nenhum
 * turno foi medido — zero diria que foi medido e não produziu nada.
 */
internal fun formatThroughput(throughput: OutputThroughput?): String {
    val rate = throughput?.tokensPerSecond ?: return "—"
    return "${rate.roundToLong()} tok/s"
}
