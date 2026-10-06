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
 * Resolves the resultaattypen that a zaaktype configuration references. A reference holds the UUID and the
 * omschrijving of its resultaattype; the omschrijving decides, and the UUID is the fallback while the omschrijving is
 * not filled yet.
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

    fun fillOmschrijvingen(zaaktypeConfiguration: ZaaktypeConfiguration) {
        zaaktypeConfiguration.nietOntvankelijkResultaattypeOmschrijving =
            zaaktypeConfiguration.nietOntvankelijkResultaattype?.let(::readOmschrijving)
        zaaktypeConfiguration.getZaakbeeindigParameters().forEach {
            it.resultaattypeOmschrijving = readOmschrijving(it.resultaattype)
        }
    }

    fun readNietOntvankelijkResultaattype(zaaktypeConfiguration: ZaaktypeConfiguration): ResultaatType? =
        zaaktypeConfiguration.nietOntvankelijkResultaattype?.let {
            resolve(
                zaaktypeUuid = zaaktypeConfiguration.zaaktypeUuid,
                resultaattypeUuid = it,
                resultaattypeOmschrijving = zaaktypeConfiguration.nietOntvankelijkResultaattypeOmschrijving
            )
        }

    fun readResultaattype(zaaktypeCompletionParameters: ZaaktypeCompletionParameters): ResultaatType =
        resolve(
            zaaktypeUuid = zaaktypeCompletionParameters.zaaktypeConfiguration.zaaktypeUuid,
            resultaattypeUuid = zaaktypeCompletionParameters.resultaattype,
            resultaattypeOmschrijving = zaaktypeCompletionParameters.resultaattypeOmschrijving
        )

    private fun readOmschrijving(resultaattypeUuid: UUID) = ztcClientService.readResultaattype(resultaattypeUuid).omschrijving

    private fun resolve(zaaktypeUuid: UUID, resultaattypeUuid: UUID, resultaattypeOmschrijving: String?): ResultaatType =
        resultaattypeOmschrijving?.let { omschrijving ->
            ztcClientService.readResultaattypen(ztcClientService.readZaaktype(zaaktypeUuid).url)
                .firstOrNull { it.omschrijving == omschrijving }
                ?: null.also {
                    LOG.warning {
                        "Zaaktype with UUID '$zaaktypeUuid' has no resultaattype with omschrijving '$omschrijving'. " +
                            "Using the resultaattype with UUID '$resultaattypeUuid'"
                    }
                }
        } ?: ztcClientService.readResultaattype(resultaattypeUuid)
}
