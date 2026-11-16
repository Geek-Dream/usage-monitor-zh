package com.usagemonitor.presentation.ui.report

import com.usagemonitor.presentation.ui.localized

import com.usagemonitor.domain.entity.ApiSource
import com.usagemonitor.domain.entity.AppLanguage
import com.usagemonitor.domain.entity.HistoryRange
import com.usagemonitor.domain.entity.UsageHistorySeries
import com.usagemonitor.domain.entity.UsageUnit
import com.usagemonitor.domain.entity.isObservedActivitySource
import com.usagemonitor.presentation.ui.*
import com.usagemonitor.presentation.viewmodel.HistoryUiState
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun historyReportRequest(state: HistoryUiState.Success, language: AppLanguage, now: Instant): UsageExportRequest {
    val date = now.toLocalDateTime(TimeZone.of("America/Sao_Paulo")).date.toString()
    val range = when (state.report.range) {
        HistoryRange.LAST_24_HOURS -> "24h"
        HistoryRange.LAST_7_DAYS -> "7d"
        HistoryRange.LAST_30_DAYS -> "30d"
        HistoryRange.TOTAL -> "total"
    }
    return UsageExportRequest("usage-monitor-history-${state.report.source.name.lowercase()}-$range-$date.pdf", UsageExportPayload.Report(reportForHistory(state, language, now), language))
}

/** Documento do snapshot estabilizado; expansão e zoom não alteram a seleção. */
fun reportForHistory(state: HistoryUiState.Success, language: AppLanguage, now: Instant): UsageReportDocument {
    val pt = language == AppLanguage.PT
    val report = state.report
    val selected = selectedHistorySeries(state)
    val sections = mutableListOf<UsageReportSection>()
    if (report.source.isObservedActivitySource()) {
        buildOpenCodeHistoryGroups(report.series, report.range).forEach { model ->
            sections += UsageReportSection.Paragraphs(model.modelName, listOf("${localized(language, "Modelo", "Model")}: ${model.modelName}") + observedMetricEntries(model, language).map { "${it.label}: ${it.value}" })
        }
        selected.forEach { series -> addPointSection(sections, series, language) }
    } else {
        selected.forEach { series ->
            val entries = if (report.source == ApiSource.DEEPSEEK) deepSeekMetricEntries(series, language, report.lastUpdatedAt)
                else historyMetricEntries(report.source, series, language, report.lastUpdatedAt)
            sections += UsageReportSection.Paragraphs(series.quotaLabel, listOf("${localized(language, "Série", "Series")}: ${series.quotaLabel}") + entries.map { "${it.label}: ${it.value}" })
            if (report.source == ApiSource.DEEPSEEK) sections += UsageReportSection.Paragraphs(localized(language, "Previsão", "Forecast"), listOf(deepSeekForecastText(series.forecast, language)))
            addWindowSections(sections, series, language)
            addPointSection(sections, series, language)
        }
    }
    val points = selected.flatMap { it.points }
    val first = points.minOfOrNull { it.capturedAt }
    val last = points.maxOfOrNull { it.capturedAt }
    val period = if (report.range == HistoryRange.TOTAL) {
        localized(language, "Todo o histórico disponível", "All available history")
    } else if (report.rangeStartsAt != null && report.rangeEndsAt != null) {
        "${historyReportTime(report.rangeStartsAt)} — ${historyReportTime(report.rangeEndsAt)}"
    } else {
        localized(language, "Recorte absoluto não registrado", "Absolute range not recorded")
    }
    val notes = mutableListOf(
        localized(language, "Observações disponíveis: ${historyReportTime(first)} — ${historyReportTime(last)}. Última coleta: ${historyReportTime(report.lastUpdatedAt)}.", "Available observations: ${historyReportTime(first)} — ${historyReportTime(last)}. Last snapshot: ${historyReportTime(report.lastUpdatedAt)}."),
        localized(language, "O início de cada janela é a primeira leitura observada. Dados ausentes não representam zero. As janelas não são limitadas às oito linhas da tela.", "Window start is the first observed reading. Missing data is not zero. Windows are not limited to the eight on-screen rows.")
    )
    if (report.range == HistoryRange.TOTAL) notes += localized(language, "Os pontos do gráfico em Total podem estar amostrados em até 720 por série. Este relatório não é uma exportação dos snapshots brutos.", "Total chart points may be sampled to 720 per series. This report is not a raw snapshot export.")
    if (report.source.isObservedActivitySource()) notes += localized(language, "Atividade observada localmente; a fonte não informa limite ou reinício de cota.", "Locally observed activity; the source does not report a quota limit or reset.")
    val account = report.accountContext?.displayLabel ?: sourceLabel(report.source)
    val quotaLabels = quotaViewLabels(report, language)
    val quota = quotaLabels?.get(state.selectedQuotaView.ordinal)
    sections.add(0, UsageReportSection.Paragraphs(localized(language, "Contexto do relatório", "Report context"), listOf(
        "${localized(language, "Fonte", "Source")}: ${sourceLabel(report.source)}",
        "${localized(language, "Conta", "Account")}: $account",
        "${localized(language, "Intervalo", "Range")}: $period${quota?.let { " · $it" } ?: ""}",
        "${localized(language, "Gerado", "Generated")}: ${historyReportTime(now)}"
    )))
    return UsageReportDocument(
        title = localized(language, "Histórico de uso", "Usage history"),
        subtitle = "${sourceLabel(report.source)} · $account · ${rangeLabel(report.range, language)}${quota?.let { " · $it" } ?: ""} · ${localized(language, "Gerado", "Generated")}: ${formatInstant(now)}",
        period = period, sections = sections, footnotes = notes
    ).sanitized()
}

private fun addWindowSections(sections: MutableList<UsageReportSection>, series: UsageHistorySeries, language: AppLanguage) {
    val pt = language == AppLanguage.PT
    if (series.windows.isNotEmpty()) sections += UsageReportSection.Table(
        heading = "${localized(language, "Janelas", "Windows")} · ${series.quotaLabel}",
        columns = listOf(UsageReportColumn(localized(language, "Início observado", "First observed"), 2f), UsageReportColumn(localized(language, "Ativa", "Active"), 2f), UsageReportColumn(localized(language, "Pico", "Peak"), alignEnd = true), UsageReportColumn(localized(language, "Esgotou em", "Exhausted after"), alignEnd = true), UsageReportColumn(localized(language, "Ritmo", "Pace"), alignEnd = true)),
        rows = series.windows.asReversed().map { listOf(windowStartLabel(it, language), activeSpanLabel(it, language), "${it.peakPercent} %", exhaustionLabel(it), paceLabel(it.averagePercentPerHour, language)) }
    )
    val distribution = series.hourlyDistribution ?: return
    sections += UsageReportSection.Table(
        heading = localized(language, "Consumo por hora do dia (BRT)", "Usage by hour of day (BRT)"),
        columns = listOf(UsageReportColumn(localized(language, "Hora", "Hour")), UsageReportColumn(localized(language, "Pontos percentuais", "Percentage points"), alignEnd = true)),
        rows = distribution.percentByHour.mapIndexed { hour, value -> listOf("${hour}h", trimDecimal(value)) },
        note = hourlyPeakLabel(distribution, language)
    )
}

private fun historyReportTime(value: Instant?): String {
    if (value == null) return "—"
    val year = value.toLocalDateTime(TimeZone.of("America/Sao_Paulo")).year
    return "$year · ${formatInstant(value)}"
}

private fun addPointSection(sections: MutableList<UsageReportSection>, series: UsageHistorySeries, language: AppLanguage) {
    if (series.points.isEmpty()) return
    val pt = language == AppLanguage.PT
    sections += UsageReportSection.Table(
        heading = "${localized(language, "Leituras", "Readings")} · ${series.quotaLabel}",
        columns = listOf(UsageReportColumn(localized(language, "Coleta (BRT)", "Collected (BRT)"), 2f), UsageReportColumn(localized(language, "Valor observado", "Observed value"), alignEnd = true), UsageReportColumn(localized(language, "Unidade", "Unit"))),
        rows = series.points.map { point -> listOf(historyReportTime(point.capturedAt), when (series.unit) { UsageUnit.CURRENCY_USD -> formatCents(point.displayUsed); UsageUnit.PERCENTAGE -> currentUsagePercent(point.displayUsed, point.displayTotal); else -> formatQuantity(point.displayUsed) }, unitSuffix(series.unit)) }
    )
}
