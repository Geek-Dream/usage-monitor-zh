package com.usagemonitor.presentation.ui

import com.usagemonitor.domain.entity.AppLanguage
import com.usagemonitor.domain.entity.ComparisonRow
import com.usagemonitor.domain.entity.PeriodType
import com.usagemonitor.domain.entity.QuotaWindowSummary
import com.usagemonitor.domain.entity.TelegramAccountHistory
import com.usagemonitor.domain.entity.UsageUnit
import com.usagemonitor.domain.entity.displayName
import kotlin.math.roundToInt
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Leituras do índice CLI e do histórico no bot (#408): vazão, semana, janelas e
 * a comparação. Mesmo HTML e mesmo escape de [TelegramBotMessages].
 */
internal object TelegramBotUsageMessages {

    /**
     * `/vazao` (AA5): tokens de saída por segundo por modelo, **ponta a ponta** — a
     * vazão do conjunto é total sobre total (`OutputThroughput`), nunca média de
     * razões. [rows] `null` é índice indisponível; linha sem vazão medida fica fora.
     */
    fun throughput(rows: List<ComparisonRow>?, language: AppLanguage): String {
        if (rows == null) {
            return localized(language, "Não deu para ler o índice das sessões CLI agora.", "Could not read the CLI session index right now.")
        }
        val measured = rows
            .mapNotNull { row -> row.throughput?.tokensPerSecond?.let { speed -> row to speed } }
            .sortedWith(compareByDescending<Pair<ComparisonRow, Double>> { (_, speed) -> speed }.thenBy { (row, _) -> row.source.ordinal }.thenBy { (row, _) -> row.label })
        if (measured.isEmpty()) {
            return localized(language, "⚡ Sem vazão medida nas últimas 24 h.", "⚡ No throughput measured in the last 24 h.")
        }
        val lines = measured.map { (row, speed) ->
            val turns = row.turnCount?.let { count -> " · $count " + localized(language, "turnos", "turns") }.orEmpty()
            "<b>${TelegramBotMessages.escape(row.source.displayName())} · ${TelegramBotMessages.escape(row.label)}</b> — " +
                "<b>${speed.roundToInt()} tok/s</b>$turns"
        }
        val header = localized(language, "<b>⚡ Vazão de saída — 24 h</b>", "<b>⚡ Output throughput — 24 h</b>")
        val footer = localized(language, "<i>ponta a ponta: do pedido à última linha</i>", "<i>end to end: from the request to the last line</i>")
        return (listOf(header) + lines + footer).joinToString("\n")
    }

    /**
     * `/janelas` (AA12): as últimas janelas de cada conta — pico, se esgotou e quanto
     * tempo o uso ficou subindo (faixa ativa, #382). [weekly] escolhe as janelas
     * semanais; sem ele, as curtas (5 h). Mesmas `QuotaWindowSummary` do Histórico.
     */
    fun windows(histories: List<TelegramAccountHistory>, weekly: Boolean, language: AppLanguage): String {
        val period = if (weekly) PeriodType.WEEKLY else PeriodType.INTERVAL
        val blocks = histories.flatMap { history ->
            history.report.series.filter { series -> series.periodType == period && series.windows.isNotEmpty() }.map { series ->
                val recent = series.windows.sortedByDescending { window -> window.resetsAt }.take(MAX_WINDOWS)
                val title = "<b>${TelegramBotMessages.escape(history.label)} · ${TelegramBotMessages.escape(displayQuotaLabel(series.quotaLabel, language))}</b>"
                title + "\n" + recent.joinToString("\n") { window -> windowLine(window, language) }
            }
        }
        if (blocks.isEmpty()) {
            val which = if (weekly) (localized(language, "semanais", "weekly")) else (localized(language, "de 5 h", "5 h"))
            return localized(language, "🪟 Sem janelas $which no histórico ainda.", "🪟 No $which windows in the history yet.")
        }
        val header = if (weekly) {
            localized(language, "<b>🪟 Janelas semanais</b>", "<b>🪟 Weekly windows</b>")
        } else {
            localized(language, "<b>🪟 Janelas de 5 h</b>", "<b>🪟 5 h windows</b>")
        }
        return header + "\n\n" + blocks.joinToString("\n\n")
    }

    private fun windowLine(window: QuotaWindowSummary, language: AppLanguage): String {
        val range = "<code>${dayClock(window.firstObservedAt, language)}–${TelegramBotMessages.clock(window.resetsAt)}</code>"
        val peak = localized(language, " pico ", " peak ") + "${window.peakPercent}%"
        val exhausted = window.exhaustedAt?.let { at -> localized(language, " · esgotou ", " · ran out ") + TelegramBotMessages.clock(at) }.orEmpty()
        val from = window.activeFrom
        val until = window.activeUntil
        val active = if (from != null && until != null && until > from) {
            localized(language, " · ativa ", " · active ") + TelegramBotInsightMessages.span(until - from)
        } else {
            ""
        }
        val open = if (window.isOpen) localized(language, " · aberta", " · open") else ""
        return "$range$peak$exhausted$active$open"
    }

    /** "13/10 11:00": o dia ajuda a separar janelas de dias diferentes com a mesma hora. */
    private fun dayClock(at: Instant, language: AppLanguage): String {
        val local = at.toLocalDateTime(SAO_PAULO)
        val day = local.date.dayOfMonth.toString().padStart(2, '0')
        val month = local.date.monthNumber.toString().padStart(2, '0')
        val date = localized(language, "$day/$month", "$month/$day")
        return "$date ${TelegramBotMessages.clock(at)}"
    }

    /** Legenda da imagem do `/comparar` (AA6): o recorte e a regra da cor. */
    fun compareCaption(days: Int, language: AppLanguage): String = localized(
        language,
        if (days == 1) {
            "<b>Comparação · últimos 1 dia</b>\n<i>cor: valor sobre o maior da coluna; o número está sempre escrito</i>"
        } else {
            "<b>Comparação · últimos $days dias</b>\n<i>cor: valor sobre o maior da coluna; o número está sempre escrito</i>"
        },
        if (days == 1) {
            "<b>Comparison · last 1 day</b>\n<i>color: value over the column maximum; the number is always written</i>"
        } else {
            "<b>Comparison · last $days days</b>\n<i>color: value over the column maximum; the number is always written</i>"
        }
    )

    /** Legenda do arquivo do `/exportar` (AA23): o recorte e o que está dentro. */
    fun exportCaption(days: Int, language: AppLanguage): String = localized(
        language,
        if (days == 1) {
            "<b>Uso do Claude CLI · último 1 dia</b>\n<i>por modelo e ferramenta, com os totais; sem projeto, branch ou conteúdo</i>"
        } else {
            "<b>Uso do Claude CLI · últimos $days dias</b>\n<i>por modelo e ferramenta, com os totais; sem projeto, branch ou conteúdo</i>"
        },
        if (days == 1) {
            "<b>Claude CLI usage · last 1 day</b>\n<i>by model and tool, with totals; no project, branch or content</i>"
        } else {
            "<b>Claude CLI usage · last $days days</b>\n<i>by model and tool, with totals; no project, branch or content</i>"
        }
    )

    /** Nome do arquivo do `/exportar`: data local do envio e o recorte. */
    fun exportFileName(now: Instant, days: Int, extension: String): String {
        val date = now.toLocalDateTime(SAO_PAULO).date
        return "usage-monitor-$date-${days}d.$extension"
    }

    /** Legenda do `/status img` (AA14): a hora da coleta, como o `/status`. */
    fun ringsCaption(collectedAt: Instant?, language: AppLanguage): String {
        val collected = collectedAt?.let { at -> (localized(language, "coleta ", "collected ")) + TelegramBotMessages.clock(at) + " BRT" }
            ?: localized(language, "sem coleta ainda", "not collected yet")
        return "<b>📊 Usage Monitor</b>\n<i>$collected</i>"
    }

    fun ringsEmpty(language: AppLanguage): String =
        localized(language, "Nenhuma conta com cota em percentual para desenhar. Use /status.", "No account with a percentage quota to draw. Use /status.")

    /** Servidor sem `libGL`, `libX11` e `fontconfig` (#440): o Skia não carrega. */
    fun imagesUnavailable(language: AppLanguage): String =
        localized(
            language,
            "Este computador não desenha imagens: faltam as bibliotecas libGL, libX11 e fontconfig. Use /status.",
            "This computer cannot draw images: the libGL, libX11 and fontconfig libraries are missing. Use /status."
        )

    fun compareEmpty(language: AppLanguage): String =
        localized(language, "Nenhum uso no recorte para comparar.", "No usage in range to compare.")

    private const val MAX_WINDOWS = 4
    private val SAO_PAULO = TimeZone.of("America/Sao_Paulo")

    /**
     * `/semana` (AA7): o consumo dos últimos 7 dias contra os 7 anteriores, por cota,
     * em pontos somados (o `UsagePeriodComparison` do histórico: reinício conta zero,
     * nunca delta negativo). Seta **e** número; saldo em dinheiro fica fora.
     */
    fun week(histories: List<TelegramAccountHistory>, language: AppLanguage): String {
        val pt = language == AppLanguage.PT
        val lines = histories.flatMap { history ->
            history.report.series.mapNotNull { series ->
                val comparison = series.comparison ?: return@mapNotNull null
                val total = series.currentDisplayTotal
                if (total <= 0L || series.unit == UsageUnit.CURRENCY_USD) return@mapNotNull null
                val current = comparison.currentDelta * 100 / total
                val previous = comparison.previousDelta * 100 / total
                val diff = current - previous
                val trend = when {
                    diff > 0 -> "▲ +$diff pp"
                    diff < 0 -> "▼ −${-diff} pp"
                    else -> "="
                }
                val name = "<b>${TelegramBotMessages.escape(history.label)} · ${TelegramBotMessages.escape(displayQuotaLabel(series.quotaLabel, language))}</b>"
                localized(language, "$name +$current pp (antes +$previous pp) $trend", "$name +$current pp (before +$previous pp) $trend")
            }
        }
        if (lines.isEmpty()) {
            return localized(language, "📅 Ainda não há duas semanas de histórico para comparar.", "📅 There are not two weeks of history to compare yet.")
        }
        val header = localized(language, "<b>📅 Últimos 7 dias × 7 anteriores</b>", "<b>📅 Last 7 days × the 7 before</b>")
        val footer = localized(language, "<i>pontos da cota somados; reinício conta zero</i>", "<i>quota points added up; a reset counts zero</i>")
        return (listOf(header) + lines + footer).joinToString("\n")
    }
}
