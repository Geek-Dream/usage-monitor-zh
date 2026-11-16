package com.usagemonitor.presentation.ui

import com.usagemonitor.domain.entity.AnalysisView
import com.usagemonitor.domain.entity.AnalysisViewKind
import com.usagemonitor.domain.entity.AnalysisWindowView
import com.usagemonitor.domain.entity.AppLanguage
import com.usagemonitor.domain.entity.BotTap
import com.usagemonitor.domain.entity.SessionAnalysis
import com.usagemonitor.domain.entity.data
import kotlin.math.roundToInt

/**
 * Textos da análise da sessão no bot (rodada AB). Toda vista sai no idioma do app; número
 * com uma casa usa vírgula em português e ponto em inglês. `null` (não medido) vira "—".
 */
internal object TelegramAnalysisMessages {

    /** As vistas que já têm texto, imagem ou arquivo; só elas viram aba. */
    val availableViews: List<AnalysisView> = listOf(
        AnalysisView.PANEL, AnalysisView.TREND, AnalysisView.INTERVALS, AnalysisView.SENTENCES, AnalysisView.TIMELINE, AnalysisView.VERSUS,
        AnalysisView.GAUGES, AnalysisView.HOURS, AnalysisView.HEATMAP, AnalysisView.CURVE, AnalysisView.DAYS, AnalysisView.CARD,
        AnalysisView.REPORT
    )

    fun pickAccount(language: AppLanguage): String =
        localized(language, "<b>📐 Análise de qual conta?</b>", "<b>📐 Analysis of which account?</b>")

    /** Botão do `/status` que abre a análise (B19). */
    fun openButton(language: AppLanguage): String = localized(language, "📐 Análise", "📐 Analysis")

    /** Botão da resposta do `/conta` que abre a análise daquela conta (B19). */
    fun accountButton(language: AppLanguage): String = localized(language, "📐 Analisar esta conta", "📐 Analyze this account")

    /** Botão "Todas" da escolha de conta: abre o placar (AB13). */
    fun scoreboardButton(language: AppLanguage): String = localized(language, "📊 Todas (placar)", "📊 All (scoreboard)")

    fun noAccount(language: AppLanguage): String = localized(language, "📐 A análise cobre contas Anthropic e Codex, e nenhuma está com leitura agora.", "📐 The analysis covers Anthropic and Codex accounts, and none has a reading right now.")

    fun notReady(language: AppLanguage): String = localized(language, "📐 Sem histórico desta conta ainda. A análise aparece depois de algumas coletas.", "📐 No history for this account yet. The analysis shows up after a few collections.")

    /** As abas (AB12): uma por vista disponível, a corrente marcada; três por linha. */
    fun tabs(accountKey: String, current: AnalysisView, language: AppLanguage): List<List<Pair<String, String>>> =
        availableViews.map { view ->
            val name = viewName(view, language)
            (if (view == current) "• $name •" else name) to BotTap.AnalysisShow(accountKey, view).data()
        }.chunked(TABS_PER_ROW)

    fun viewName(view: AnalysisView, language: AppLanguage): String {
        val pt = language == AppLanguage.PT
        return when (view) {
            AnalysisView.PANEL -> localized(language, "Painel", "Panel")
            AnalysisView.TREND -> localized(language, "Tendência", "Trend")
            AnalysisView.INTERVALS -> localized(language, "Intervalos", "Intervals")
            AnalysisView.SENTENCES -> localized(language, "Frases", "Reading")
            AnalysisView.TIMELINE -> localized(language, "Janela 5h", "5h window")
            AnalysisView.VERSUS -> localized(language, "Antes × agora", "Before × now")
            AnalysisView.GAUGES -> localized(language, "🖼 Velocímetro", "🖼 Gauges")
            AnalysisView.HOURS -> localized(language, "🖼 Horas", "🖼 Hours")
            AnalysisView.HEATMAP -> localized(language, "🖼 Dia × hora", "🖼 Day × hour")
            AnalysisView.CURVE -> localized(language, "🖼 Curva", "🖼 Curve")
            AnalysisView.DAYS -> localized(language, "🖼 Dias", "🖼 Days")
            AnalysisView.CARD -> localized(language, "🖼 Cartão", "🖼 Card")
            AnalysisView.REPORT -> "📄 PDF"
        }
    }

    /** O texto de uma vista de texto; `null` para vista de imagem ou arquivo, ou ainda sem texto. */
    fun text(view: AnalysisView, analysis: SessionAnalysis, language: AppLanguage): String? {
        if (view.kind != AnalysisViewKind.TEXT || view !in availableViews) return null
        return when (view) {
            AnalysisView.PANEL -> panel(analysis, language)
            AnalysisView.TREND -> TelegramAnalysisTexts.trend(analysis, language)
            AnalysisView.INTERVALS -> TelegramAnalysisTexts.intervals(analysis, language)
            AnalysisView.SENTENCES -> TelegramAnalysisTexts.sentences(analysis, language)
            AnalysisView.TIMELINE -> TelegramAnalysisTexts.timeline(analysis, language)
            AnalysisView.VERSUS -> TelegramAnalysisTexts.versus(analysis, language)
            else -> null
        }
    }

    /**
     * AB1 · painel de bordo: um bloco por janela com consumo, ritmo agora, médio e seguro, e
     * onde ela chega no reinício; a vazão da última hora por modelo no fim.
     */
    fun panel(analysis: SessionAnalysis, language: AppLanguage): String {
        val blocks = listOfNotNull(
            analysis.fiveHour?.let { window -> windowBlock(localized(language, "JANELA 5h", "5h WINDOW"), window, analysis, language) },
            analysis.weekly?.let { window -> windowBlock(localized(language, "SEMANAL", "WEEKLY"), window, analysis, language) }
        )
        val speeds = analysis.modelSpeeds.joinToString(" · ") { speed -> "${speed.model} ${speed.tokensPerSecond.roundToInt()}" }
            .ifEmpty { localized(language, "sem medição", "not measured") }
        val body = (blocks + ((localized(language, "TOKENS/s (1 h)  ", "TOKENS/s (1 h)  ")) + speeds)).joinToString("\n\n")
        return header(analysis, language) + "\n<pre>" + TelegramBotMessages.escape(body) + "</pre>"
    }

    private fun windowBlock(title: String, window: AnalysisWindowView, analysis: SessionAnalysis, language: AppLanguage): String {
        val pt = language == AppLanguage.PT
        val reset = window.resetsAt?.let { at -> localized(language, "reinicia ", "resets ") + TelegramBotMessages.resetLabel(at, analysis.generatedAt, pt) }.orEmpty()
        val since = window.startsAt?.let { at -> localized(language, "desde ", "since ") + TelegramBotMessages.clock(at) }.orEmpty()
        val current = window.paceNow ?: window.paceAverage
        val safe = window.safePace
        val verdict = when {
            current == null || safe == null -> ""
            current <= safe -> localized(language, "  → folga", "  → room left")
            else -> localized(language, "  → acima", "  → too fast")
        }
        return listOf(
            "${title.padEnd(11)} ${"${window.percent}%".padStart(4)}   $reset".trimEnd(),
            "  " + localized(language, "agora ", "now   ") + "  ${pace(window.paceNow, language)} pp/h  " + localized(language, "(última 1 h)", "(last 1 h)"),
            "  " + localized(language, "média ", "avg   ") + "  ${pace(window.paceAverage, language)} pp/h  ${if (since.isEmpty()) "" else "($since)"}".trimEnd(),
            "  " + localized(language, "seguro", "safe  ") + "  ${pace(window.safePace, language)} pp/h$verdict",
            "  " + localized(language, "no reset ≈ ", "at reset ≈ ") + (window.projectedAtReset?.let { value -> "$value%" } ?: "—")
        ).joinToString("\n")
    }

    private fun header(analysis: SessionAnalysis, language: AppLanguage): String =
        "<b>📐 ${TelegramBotMessages.escape(analysis.accountLabel)}</b> · <i>" + localized(language, "análise ", "analysis ") +
            TelegramBotMessages.clock(analysis.generatedAt) + " BRT</i>"

    /** Uma casa, vírgula em português; `null` vira "—", nunca zero. */
    fun pace(value: Double?, language: AppLanguage): String {
        if (value == null) return "—".padStart(5)
        val tenths = (value * 10).roundToInt()
        val text = "${tenths / 10}${localized(language, ",", ".")}${kotlin.math.abs(tenths % 10)}"
        return text.padStart(5)
    }

    fun pace(value: Double?, pt: Boolean): String =
        pace(value, if (pt) AppLanguage.PT else AppLanguage.EN)

    private const val TABS_PER_ROW = 3
}
