package com.usagemonitor.presentation.ui

import com.usagemonitor.domain.entity.ApiSource
import com.usagemonitor.domain.entity.AppLanguage
import com.usagemonitor.domain.entity.UsageHistorySeries
import com.usagemonitor.domain.entity.UsageUnit
import com.usagemonitor.domain.entity.isObservedActivitySource
import com.usagemonitor.presentation.viewmodel.HistoryQuotaView
import com.usagemonitor.presentation.viewmodel.HistoryUiState
import kotlin.math.roundToLong
import kotlin.time.Instant

/** Mesmos pares de dados para a tela e o PDF, sem dependência do desenho. */
internal fun deepSeekMetricEntries(series: UsageHistorySeries, language: AppLanguage, referenceAt: Instant?): List<HistoryMetricEntry> {
    val pt = language == AppLanguage.PT
    return buildList {
        add(HistoryMetricEntry(CommonLabels.currentBalance(language), formatCents(series.currentDisplayUsed)))
        add(HistoryMetricEntry(localized(language, "Gasto no período", "Spent in range"), formatCents(series.deltaDisplayUsed)))
        add(HistoryMetricEntry(localized(language, "Ritmo médio", "Average pace"), formatCents(series.averageDisplayConsumptionPerHour.toLong()) + "/h"))
        if (referenceAt != null) add(HistoryMetricEntry(localized(language, "Última coleta", "Last snapshot"), formatInstant(referenceAt)))
    }
}

internal fun observedMetricEntries(model: OpenCodeHistoryModelReport, language: AppLanguage): List<HistoryMetricEntry> {
    val pt = language == AppLanguage.PT
    val tokens = model.chartSeries.unit == UsageUnit.TOKENS
    val noun = if (tokens) "Tokens" else localized(language, "Requisições", "Requests")
    fun value(count: Long): String = if (tokens) "${formatQuantity(count)} tok" else localizedRequests(count, language)
    return listOf(
        HistoryMetricEntry(localized(language, "$noun nas últimas 5h", "$noun in last 5h"), value(model.requests5h)),
        HistoryMetricEntry(localized(language, "$noun nos últimos 7 dias", "$noun in last 7 days"), value(model.requests7d)),
        HistoryMetricEntry(localized(language, "Variação observada", "Observed change"), value(model.chartSeries.deltaDisplayUsed)),
        HistoryMetricEntry(localized(language, "Média por hora", "Average per hour"), value(model.chartSeries.averageDisplayConsumptionPerHour.roundToLong()) + "/h"),
        HistoryMetricEntry(localized(language, "Limite", "Limit"), localized(language, "Não informado pela fonte", "Not reported by the source"))
    )
}

/**
 * DeepSeek e as fontes de atividade observada montam os próprios cards de histórico:
 * sem seletor de janela e sem recorte de séries por família de cota.
 */
internal fun ApiSource.buildsOwnHistoryCards(): Boolean = this == ApiSource.DEEPSEEK || isObservedActivitySource()

/** A mesma seleção de cotas do gráfico; séries independentes permanecem no relatório. */
internal fun selectedHistorySeries(state: HistoryUiState.Success): List<UsageHistorySeries> {
    if (state.report.source.buildsOwnHistoryCards()) return state.report.series
    return buildGenericHistoryGroups(state.report.series).flatMap { group ->
        val weekly = group.weeklySummary
        when {
            weekly == null -> listOf(group.chartSeries)
            state.selectedQuotaView == HistoryQuotaView.WEEKLY -> listOf(weekly)
            state.selectedQuotaView == HistoryQuotaView.INTERVAL -> listOf(group.chartSeries)
            else -> listOfNotNull(group.chartSeries, weekly, group.monthlySummary)
        }
    }
}
