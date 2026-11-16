package com.usagemonitor.presentation.ui

import com.usagemonitor.domain.entity.ApiSource
import com.usagemonitor.domain.entity.displayName
import com.usagemonitor.domain.entity.AppLanguage
import com.usagemonitor.domain.entity.ACTIVITY_TIME_ZONE_ID
import com.usagemonitor.presentation.viewmodel.DashboardToast
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal fun decodeToastMessage(toast: DashboardToast, language: AppLanguage): String {
    return when (toast) {
        is DashboardToast.RateLimit -> {
            val sourceLabel = sourceLabelFromKey(toast.source)
            val retryAt = toast.retryAt
            if (retryAt != null) {
                val clock = formatRetryClock(retryAt)
                return localized(language, "$sourceLabel limitado temporariamente - aguarde até $clock BRT.", "$sourceLabel temporarily limited - wait until $clock BRT.")
            }
            localized(language, "$sourceLabel limitado temporariamente - aguardando próxima atualização...", "$sourceLabel temporarily limited - waiting for the next refresh...")
        }

        is DashboardToast.ServiceUnavailable -> {
            val sourceLabel = sourceLabelFromKey(toast.source)
            localized(language, "$sourceLabel temporariamente indisponível - tente novamente em instantes.", "$sourceLabel is temporarily unavailable - retry in a few moments.")
        }

        is DashboardToast.ApiError -> {
            val sourceLabel = sourceLabelFromKey(toast.source)
            "$sourceLabel: ${toast.message}"
        }

        is DashboardToast.ReleasePageError -> {
            localized(language, "Não foi possível abrir a página da release. ${toast.message}", "Could not open the release page. ${toast.message}")
        }
    }
}

internal fun sourceLabelFromKey(source: ApiSource): String = source.displayName()

/** A hora de [instant] em BRT, `HH:mm` — o prazo do backoff no toast e no banner (issue #269). */
internal fun formatRetryClock(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.of(ACTIVITY_TIME_ZONE_ID))
    return "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
}
