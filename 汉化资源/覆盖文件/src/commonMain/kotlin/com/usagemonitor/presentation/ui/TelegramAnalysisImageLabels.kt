package com.usagemonitor.presentation.ui

import com.usagemonitor.domain.entity.AnalysisView
import com.usagemonitor.domain.entity.AppLanguage
import com.usagemonitor.domain.entity.SessionAnalysis
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

/**
 * Os textos que vão **dentro** das imagens da análise (rodada AB) e as legendas delas, no
 * idioma do app. Moram no `commonMain` para o teste conferir as palavras sem desenhar nada;
 * o renderizador só posiciona.
 */
internal object TelegramAnalysisImageLabels {

    fun title(view: AnalysisView, analysis: SessionAnalysis, language: AppLanguage): String {
        val pt = language == AppLanguage.PT
        val what = when (view) {
            AnalysisView.GAUGES -> localized(language, "ritmo agora (última 1 h)", "pace now (last 1 h)")
            AnalysisView.HOURS -> localized(language, "pp da 5h por hora · últimas 24 h", "5h pp per hour · last 24 h")
            AnalysisView.HEATMAP -> localized(language, "pp da 5h por hora · 7 dias (BRT)", "5h pp per hour · 7 days (BRT)")
            AnalysisView.CURVE -> localized(language, "janela 5h e ritmo uniforme", "5h window and even pace")
            AnalysisView.DAYS -> localized(language, "pp da semanal por dia", "weekly pp per day")
            AnalysisView.CARD -> localized(language, "análise", "analysis")
            else -> ""
        }
        return "${analysis.accountLabel} — $what"
    }

    /** O rótulo de baixo de cada mostrador: a janela, o percentual e o ritmo seguro. */
    fun gaugeLabel(window: String, percent: Int, safe: Double?, language: AppLanguage): Pair<String, String> {
        val pt = language == AppLanguage.PT
        val safeText = safe?.let { value -> (localized(language, "seguro até ", "safe up to ")) + TelegramAnalysisMessages.pace(value, pt).trim() } ?: localized(language, "sem reinício", "no reset")
        return "$window · $percent%" to safeText
    }

    fun window5h(language: AppLanguage): String = "5h"

    fun windowWeekly(language: AppLanguage): String = localized(language, "semanal", "weekly")

    /** Os textos da curva da janela (AB9): a reta uniforme, o ponto de agora e o fim projetado. */
    class CurveLabels(val even: String, val now: String, val atReset: String)

    fun curveLabels(percent: Int, projected: Int?, language: AppLanguage): CurveLabels {
        val pt = language == AppLanguage.PT
        return CurveLabels(
            even = localized(language, "ritmo uniforme · 20 pp/h", "even pace · 20 pp/h"),
            now = (localized(language, "agora ", "now ")) + "$percent%",
            atReset = projected?.let { value -> localized(language, "≈$value% no reinício", "≈$value% at reset") }.orEmpty()
        )
    }

    enum class TileTone { PLAIN, SPEED, GOOD, WARNING }

    /** Um bloco do cartão (AB11): o número grande, o que ele é e o contexto embaixo. */
    class CardTile(val big: String, val label: String, val detail: String, val tone: TileTone)

    /**
     * Os seis números do cartão de resultado (AB11): 5h, ritmo, semanal, vazão, contra a janela
     * anterior e a hora mais pesada. Número não medido vira "—", nunca zero.
     */
    fun cardTiles(analysis: SessionAnalysis, language: AppLanguage): List<CardTile> {
        val pt = language == AppLanguage.PT
        fun pace(value: Double?) = TelegramAnalysisMessages.pace(value, pt).trim()
        val five = analysis.fiveHour
        val weekly = analysis.weekly
        val speed = analysis.intervals.firstOrNull()?.tokensPerSecond
        val previous = analysis.previous
        val change = previous?.let { item ->
            val now = item.currentPace
            val before = item.previousPace
            if (now == null || before == null || before <= 0.0) null else ((now - before) / before * 100).roundToInt()
        }
        val peak = analysis.hours.filter { bucket -> (bucket.percentPoints ?: 0.0) > 0.0 }.maxByOrNull { bucket -> bucket.percentPoints ?: 0.0 }
        return listOf(
            CardTile(
                five?.let { window -> "${window.percent}%" } ?: "—",
                "5h" + (five?.resetsAt?.let { at -> (localized(language, " · reinicia ", " · resets ")) + TelegramBotMessages.clock(at) }.orEmpty()),
                five?.projectedAtReset?.let { value -> localized(language, "→ ≈$value% no reinício", "→ ≈$value% at reset") }.orEmpty(),
                TileTone.PLAIN
            ),
            CardTile(
                "${pace(five?.paceNow)} pp/h",
                localized(language, "ritmo 5h agora", "5h pace now"),
                (localized(language, "média da janela ", "window average ")) + pace(five?.paceAverage),
                TileTone.PLAIN
            ),
            CardTile(
                weekly?.let { window -> "${window.percent}%" } ?: "—",
                localized(language, "semanal", "weekly"),
                "${pace(weekly?.paceNow)} pp/h · " + (localized(language, "seguro ", "safe ")) + pace(weekly?.safePace),
                TileTone.PLAIN
            ),
            CardTile(
                (speed?.roundToInt()?.toString() ?: "—") + " tok/s",
                localized(language, "última 1 h", "last 1 h"),
                analysis.modelSpeeds.firstOrNull()?.let { item -> "${item.model} ${item.tokensPerSecond.roundToInt()}" }.orEmpty(),
                TileTone.SPEED
            ),
            CardTile(
                change?.let { value -> (if (value <= 0) "↓ " else "↑ ") + "${kotlin.math.abs(value)}%" } ?: "—",
                localized(language, "ritmo vs janela anterior", "pace vs previous window"),
                previous?.let { item -> (localized(language, "agora ", "now ")) + pace(item.currentPace) + (localized(language, " · antes ", " · before ")) + pace(item.previousPace) }.orEmpty(),
                if (change == null) TileTone.PLAIN else if (change <= 0) TileTone.GOOD else TileTone.WARNING
            ),
            CardTile(
                peak?.let { bucket -> TelegramBotMessages.clock(bucket.start).substring(0, 2) + "h" } ?: "—",
                localized(language, "hora mais pesada (24 h)", "heaviest hour (24 h)"),
                peak?.percentPoints?.let { value -> "${value.roundToInt()} pp" + localized(language, " em 1 h", " in 1 h") }.orEmpty(),
                TileTone.WARNING.takeIf { peak != null } ?: TileTone.PLAIN
            )
        )
    }

    /** O cabeçalho do cartão: a marca e a data/hora local da análise. */
    fun cardHeader(analysis: SessionAnalysis, language: AppLanguage): Pair<String, String> {
        val pt = language == AppLanguage.PT
        val local = analysis.generatedAt.toLocalDateTime(TimeZone.of("America/Sao_Paulo"))
        val date = TelegramBotMessages.weekday(local.dayOfWeek, pt) + " " +
            local.date.day.toString().padStart(2, '0') + "/" + local.date.month.number.toString().padStart(2, '0')
        return (localized(language, "USAGE MONITOR · ANÁLISE", "USAGE MONITOR · ANALYSIS")) to "$date · ${TelegramBotMessages.clock(analysis.generatedAt)}"
    }

    fun cardFooter(language: AppLanguage): String =
        localized(language, "só metadados de uso: nenhum prompt ou resposta sai do PC", "usage metadata only: no prompt or answer leaves the PC")

    /**
     * AB15: o nome do arquivo (fonte, data e hora locais, sem rótulo nem e-mail da conta) e a
     * legenda com o resumo em uma linha.
     */
    fun reportFileName(analysis: SessionAnalysis, language: AppLanguage): String {
        val local = analysis.generatedAt.toLocalDateTime(TimeZone.of("America/Sao_Paulo"))
        val stamp = local.date.toString() + "-" + local.hour.toString().padStart(2, '0') + local.minute.toString().padStart(2, '0')
        val prefix = localized(language, "analise", "analysis")
        return "$prefix-${analysis.source.name.lowercase()}-$stamp.pdf"
    }

    fun reportCaption(analysis: SessionAnalysis, language: AppLanguage): String {
        val pt = language == AppLanguage.PT
        val fiveHour = analysis.fiveHour
        val projected = fiveHour?.projectedAtReset
        val projection = when {
            projected == null -> ""
            pt -> " (≈$projected% no reinício)"
            else -> " (≈$projected% at reset)"
        }
        val parts = listOfNotNull(
            fiveHour?.let { window -> "5h ${window.percent}%$projection" },
            analysis.fiveHour?.paceNow?.let { value -> "${TelegramAnalysisMessages.pace(value, pt).trim()} pp/h" },
            analysis.weekly?.let { window -> (localized(language, "semanal ", "weekly ")) + "${window.percent}%" },
            analysis.modelSpeeds.takeIf { speeds -> speeds.isNotEmpty() }?.joinToString("/") { speed -> speed.tokensPerSecond.roundToInt().toString() }?.let { value -> "$value tok/s" }
        )
        return "<b>📐 ${TelegramBotMessages.escape(analysis.accountLabel)}</b> · " + parts.joinToString(" · ")
    }

    /** A legenda da média tracejada dos dias (AB10). */
    fun average(value: Double, language: AppLanguage): String {
        val pt = language == AppLanguage.PT
        return (localized(language, "--- média ", "--- average ")) + TelegramAnalysisMessages.pace(value, pt).trim() + " pp"
    }

    /** A legenda da linha azul de vazão, que tem eixo próprio à direita. */
    fun speedLegend(language: AppLanguage): String =
        localized(language, "— tok/s (eixo à direita)", "— tok/s (right axis)")

    fun notMeasured(language: AppLanguage): String = localized(language, "sem leitura", "no reading")

    /** Vazão por modelo, a linha de rodapé das imagens. */
    fun speeds(analysis: SessionAnalysis, language: AppLanguage): String {
        val pt = language == AppLanguage.PT
        if (analysis.modelSpeeds.isEmpty()) return localized(language, "tok/s: sem medição na última hora", "tok/s: not measured in the last hour")
        return "tok/s (1 h): " + analysis.modelSpeeds.joinToString(" · ") { speed -> "${speed.model} ${speed.tokensPerSecond.roundToInt()}" }
    }

    /** Legenda da foto no Telegram (HTML do bot, escapada). */
    fun caption(view: AnalysisView, analysis: SessionAnalysis, language: AppLanguage): String {
        val pt = language == AppLanguage.PT
        val note = when (view) {
            AnalysisView.GAUGES -> localized(language, "marca clara = ritmo que leva a 100% exatamente no reinício", "light mark = the pace that reaches 100% exactly at the reset")
            AnalysisView.CURVE -> localized(language, "abaixo da reta tracejada, sobra cota no reinício", "below the dashed line, quota is left at the reset")
            else -> ""
        }
        return "<b>📐 ${TelegramBotMessages.escape(analysis.accountLabel)}</b>" + (if (note.isEmpty()) "" else "\n<i>$note</i>")
    }
}
