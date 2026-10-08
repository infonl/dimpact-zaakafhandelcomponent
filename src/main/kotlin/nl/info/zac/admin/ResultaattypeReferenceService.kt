/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.ResultaatType
import nl.info.zac.admin.model.ZaaktypeCompletionParameters
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID
import java.util.logging.Logger

/**
 * Resolves the resultaattypen that a zaaktype configuration references by omschrijving, within the zaaktype
 * version of the configuration.
 */
@ApplicationScoped
@NoArgConstructor
@AllOpen
class ResultaattypeReferenceService @Inject constructor(
    private val ztcClientService: ZtcClientService
) {
    companion object {
        private val LOG = Logger.getLogger(ResultaattypeReferenceService::class.java.name)
    }

    fun readOmschrijving(resultaattypeUuid: UUID): String = ztcClientService.readResultaattype(resultaattypeUuid).omschrijving

    fun findNietOntvankelijkResultaattype(zaaktypeConfiguration: ZaaktypeConfiguration): ResultaatType? =
        zaaktypeConfiguration.nietOntvankelijkResultaattypeOmschrijving?.let {
            find(zaaktypeConfiguration.zaaktypeUuid, it)
        }

    fun findResultaattype(zaaktypeCompletionParameters: ZaaktypeCompletionParameters): ResultaatType? =
        find(
            zaaktypeCompletionParameters.zaaktypeConfiguration.zaaktypeUuid,
            zaaktypeCompletionParameters.resultaattypeOmschrijving
        )

    private fun find(zaaktypeUuid: UUID, resultaattypeOmschrijving: String): ResultaatType? =
        ztcClientService.readResultaattypen(ztcClientService.readZaaktype(zaaktypeUuid).url)
            .firstOrNull { it.omschrijving == resultaattypeOmschrijving }
            ?: null.also {
                LOG.warning {
                    "Zaaktype with UUID '$zaaktypeUuid' has no resultaattype with omschrijving '$resultaattypeOmschrijving'"
                }
            }
}
