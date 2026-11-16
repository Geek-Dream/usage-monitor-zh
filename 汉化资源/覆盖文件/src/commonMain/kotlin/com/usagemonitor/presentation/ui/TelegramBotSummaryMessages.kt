package com.usagemonitor.presentation.ui

import com.usagemonitor.domain.entity.AppLanguage
import com.usagemonitor.domain.entity.TelegramDailySpend
import com.usagemonitor.domain.entity.UsageAlertSettings
import com.usagemonitor.domain.entity.UsageSnapshot
import com.usagemonitor.domain.entity.UsageSnapshotAccount
import com.usagemonitor.domain.entity.UsageSnapshotQuota
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Resumo diário do bot (#398, direção Y1): uma linha por conta com a cota mais
 * cheia, o que reinicia hoje e o gasto do Claude Code nas últimas 24 h. Mesmo HTML
 * e mesmo escape de [TelegramBotMessages].
 */
internal object TelegramBotSummaryMessages {

    /**
     * [spend] `null` é "não medido" (índice indisponível): a linha de gasto some,
     * em vez de dizer US$ 0,00.
     */
    fun summary(snapshot: UsageSnapshot?, spend: TelegramDailySpend?, language: AppLanguage): String {
        if (snapshot == null || snapshot.accounts.isEmpty()) return TelegramBotMessages.noReading(language)
        val now = snapshot.generatedAt
        val local = now.toLocalDateTime(SAO_PAULO)
        val date = "${TelegramBotMessages.weekday(local.dayOfWeek, language)} " +
            "${local.date.dayOfMonth.toString().padStart(2, '0')}/${local.date.monthNumber.toString().padStart(2, '0')}"
        val header = (localized(language, "<b>☀ Resumo de $date</b>", "<b>☀ Summary for $date</b>"))
        val collected = snapshot.lastCollectedAt?.let { at ->
            "<i>" + (localized(language, "coleta ", "collected ")) + "${TelegramBotMessages.clock(at)} BRT · " +
                TelegramBotMessages.elapsed(now - at, language) + "</i>"
        }
        val lines = snapshot.accounts.map { account -> accountLine(account, now, language) }
        val resets = resetsToday(snapshot, now, language)
        val spendLine = spend?.let { measured -> spendLine(measured, language) }
        val footer = localized(language, "<i>/resumo a qualquer hora</i>", "<i>/summary any time</i>")
        return listOfNotNull(
            listOfNotNull(header, collected).joinToString("\n"),
            lines.joinToString("\n"),
            listOfNotNull(resets, spendLine).joinToString("\n").ifEmpty { null },
            footer
        ).joinToString("\n\n")
    }

    /**
     * Painel fixado (#398, Y5): uma linha por conta com a cota mais cheia e a barra.
     * A hora é a da **coleta**, nunca a do envio — o texto só muda quando há leitura
     * nova, e é isso que segura as edições no ritmo da coleta. A barra é a de quadrados
     * por zona do `/status` (rodada AE, AE7), com os limiares de cada conta.
     */
    fun panel(snapshot: UsageSnapshot?, language: AppLanguage, alertSettings: UsageAlertSettings = UsageAlertSettings.DEFAULT): String {
        if (snapshot == null || snapshot.accounts.isEmpty()) return TelegramBotMessages.noReading(language)
        val updated = snapshot.lastCollectedAt?.let { at -> (localized(language, "atualizado ", "updated ")) + "${TelegramBotMessages.clock(at)} BRT" }
            ?: localized(language, "sem coleta ainda", "not collected yet")
        val lines = snapshot.accounts.map { account ->
            val mark = account.worstRisk?.let { level -> TelegramBotMessages.riskEmoji(level) + " " }.orEmpty()
            val quota = fullestQuota(account)
            val value = quota?.let { item ->
                " · ${TelegramBotMessages.escape(displayQuotaLabel(item.label, language))}\n" +
                    TelegramBotMessages.quotaLine(item, language, alertSettings.quotaPercentsFor(account.targetKey))
            }.orEmpty()
            "$mark<b>${TelegramBotMessages.escape(account.label)}</b>$value"
        }
        val title = localized(language, "<b>📌 Painel ao vivo</b>", "<b>📌 Live panel</b>")
        return "$title\n<i>$updated</i>\n\n" + lines.joinToString("\n")
    }

    /**
     * Painel em uma linha (#408, AA15): ponto de risco, conta e o pior percentual (saldo
     * mostra o valor), separados por `·` — o banner do topo da conversa mostra só a primeira
     * linha, sem as marcas. A hora da coleta vai na segunda, pelo mesmo motivo de [panel].
     */
    fun compactPanel(snapshot: UsageSnapshot?, language: AppLanguage): String {
        if (snapshot == null || snapshot.accounts.isEmpty()) return TelegramBotMessages.noReading(language)
        val line = snapshot.accounts.joinToString(" · ") { account ->
            val mark = account.worstRisk?.let { level -> TelegramBotMessages.riskEmoji(level) + " " }.orEmpty()
            val quota = fullestQuota(account)
            val percent = quota?.percent
            val value = when {
                quota == null -> ""
                percent != null -> " $percent%"
                else -> " " + TelegramBotMessages.quotaValue(quota, language)
            }
            TelegramBotMessages.escape("$mark${account.label}$value")
        }
        val updated = snapshot.lastCollectedAt?.let { at -> (localized(language, "coleta ", "collected ")) + "${TelegramBotMessages.clock(at)} BRT" }
            ?: localized(language, "sem coleta ainda", "not collected yet")
        return "$line\n<i>$updated</i>"
    }

    private fun fullestQuota(account: UsageSnapshotAccount): UsageSnapshotQuota? =
        account.quotas.filter { item -> item.percent != null }.maxByOrNull { item -> item.percent ?: 0 }
            ?: account.quotas.firstOrNull()

    /** A cota mais cheia da conta; sem percentual (saldo), a primeira, com o valor que o `/status` mostra. */
    private fun accountLine(account: UsageSnapshotAccount, now: Instant, language: AppLanguage): String {
        val pt = language == AppLanguage.PT
        val mark = account.worstRisk?.let { level -> TelegramBotMessages.riskEmoji(level) + " " }.orEmpty()
        val name = "<b>${TelegramBotMessages.escape(account.label)}</b>"
        val quota = fullestQuota(account) ?: return mark + name
        val reset = quota.resetsAt?.let { at -> localized(language, " · reinicia ", " · resets ") + TelegramBotMessages.resetLabel(at, now, pt) }.orEmpty()
        return "$mark$name · ${TelegramBotMessages.escape(displayQuotaLabel(quota.label, language))} ${valueOf(quota, language)}$reset"
    }

    private fun valueOf(quota: UsageSnapshotQuota, language: AppLanguage): String {
        val percent = quota.percent
        return if (percent != null) "<b>$percent%</b>" else TelegramBotMessages.escape(TelegramBotMessages.quotaValue(quota, language))
    }

    /** Cotas que reiniciam ainda hoje (dia local), na ordem do relógio. */
    private fun resetsToday(snapshot: UsageSnapshot, now: Instant, language: AppLanguage): String? {
        val today = now.toLocalDateTime(SAO_PAULO).date
        val entries = snapshot.accounts.flatMap { account ->
            account.quotas.mapNotNull { quota ->
                val at = quota.resetsAt?.takeIf { instant -> instant > now && instant.toLocalDateTime(SAO_PAULO).date == today }
                at?.let { instant -> instant to "${TelegramBotMessages.escape(account.label)} ${TelegramBotMessages.escape(displayQuotaLabel(quota.label, language))} " + localized(language, "às ", "at ") + TelegramBotMessages.clock(instant) }
            }
        }.sortedBy { (instant, _) -> instant }
        if (entries.isEmpty()) return null
        return localized(language, "<b>Reinicia hoje</b>: ", "<b>Resets today</b>: ") + entries.joinToString(" · ") { (_, text) -> text }
    }

    private fun spendLine(spend: TelegramDailySpend, language: AppLanguage): String {
        val cents = spend.costMicros / MICROS_PER_CENT
        val value = "US$ ${cents / 100},${(cents % 100).toString().padStart(2, '0')}" + if (spend.unpricedTurns > 0) "+" else ""
        return localized(language, "<b>Claude Code, últimas 24 h</b>: $value · ${spend.sessionCount} sessões", "<b>Claude Code, last 24 h</b>: $value · ${spend.sessionCount} sessions")
    }

    private const val MICROS_PER_CENT = 10_000L
    private val SAO_PAULO = TimeZone.of("America/Sao_Paulo")
}
