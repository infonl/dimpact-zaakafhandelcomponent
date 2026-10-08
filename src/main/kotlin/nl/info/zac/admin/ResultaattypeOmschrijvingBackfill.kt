/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Initialized
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.interceptor.Interceptor
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.WebApplicationException
import nl.info.client.zgw.shared.exception.ZgwErrorException
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Fills the omschrijving of the resultaattype references that were stored before the omschrijving existed.
 * A reference whose resultaattype cannot be read stays empty, and the next start of ZAC tries it again.
 *
 * Temporary: the contract step that drops the resultaattype UUID columns removes this class.
 */
@ApplicationScoped
@NoArgConstructor
@AllOpen
class ResultaattypeOmschrijvingBackfill @Inject constructor(
    private val zaaktypeConfigurationRepository: ZaaktypeConfigurationRepository,
    private val zaaktypeConfigurationService: ZaaktypeConfigurationService,
    private val ztcClientService: ZtcClientService
) {
    companion object {
        private val LOG = Logger.getLogger(ResultaattypeOmschrijvingBackfill::class.java.name)

        // After FlywayIntegrator.onStartup, which observes the same event with the default priority
        // (APPLICATION + 500) and adds the omschrijving columns.
        private const val PRIORITY = Interceptor.Priority.APPLICATION + 1000
    }

    @Suppress("TooGenericExceptionCaught")
    fun onStartup(
        @Observes @Priority(PRIORITY) @Initialized(ApplicationScoped::class) @Suppress("UNUSED_PARAMETER") event: Any
    ) {
        try {
            backfill()
        } catch (runtimeException: RuntimeException) {
            LOG.log(Level.SEVERE, "Resultaattype omschrijving backfill failed; the next start retries it", runtimeException)
        }
    }

    fun backfill() {
        val resultaattypeUuids = zaaktypeConfigurationRepository.listResultaattypenWithoutOmschrijving()
        if (resultaattypeUuids.isEmpty()) return
        val filledReferenceCount = resultaattypeUuids.sumOf { resultaattypeUuid ->
            readOmschrijving(resultaattypeUuid)?.let {
                zaaktypeConfigurationRepository.fillResultaattypeOmschrijving(resultaattypeUuid, it)
            } ?: 0
        }
        zaaktypeConfigurationService.clearManagedCache()
        val unresolvedReferenceCount = zaaktypeConfigurationRepository.countResultaattypeReferencesWithoutOmschrijving()
        LOG.info {
            "Resultaattype omschrijving backfill: filled $filledReferenceCount resultaattype reference(s), " +
                "$unresolvedReferenceCount left unresolved"
        }
    }

    private fun readOmschrijving(resultaattypeUuid: UUID): String? =
        try {
            ztcClientService.readResultaattype(resultaattypeUuid).omschrijving
        } catch (zgwErrorException: ZgwErrorException) {
            logUnresolved(resultaattypeUuid, zgwErrorException)
        } catch (webApplicationException: WebApplicationException) {
            logUnresolved(resultaattypeUuid, webApplicationException)
        } catch (processingException: ProcessingException) {
            logUnresolved(resultaattypeUuid, processingException)
        }

    private fun logUnresolved(resultaattypeUuid: UUID, exception: RuntimeException): String? {
        LOG.warning { "Cannot read resultaattype with UUID '$resultaattypeUuid': ${exception.message}" }
        return null
    }
}
