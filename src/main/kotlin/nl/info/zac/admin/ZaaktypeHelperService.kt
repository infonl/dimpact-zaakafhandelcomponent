/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.ResultaatType
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration
import nl.info.zac.admin.model.ZaaktypeCompletionParameters
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
class ZaaktypeHelperService @Inject constructor(
    private val ztcClientService: ZtcClientService,
) {
    /**
     * Update ZaakbeeindigGegevens based on the given zaaktype
     *
     * @param zaaktypeConfiguration source
     * @param newZaaktype           zaaktype to read the results from
     */
    fun updateZaakbeeindigGegevens(
        zaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktype: ZaakType
    ) {
        val newResultaattypen = newZaaktype.resultaattypen.map { ztcClientService.readResultaattype(it) }

        zaaktypeConfiguration.nietOntvankelijkResultaattype?.let {
            mapPreviousResultaattypeToNewResultaattype(it, newResultaattypen)
        }

        val _ = zaaktypeConfiguration.getZaakbeeindigParameters().mapNotNull { zaakbeeindigParameter ->
            mapPreviousResultaattypeToNewResultaattype(
                zaakbeeindigParameter.resultaattype,
                newResultaattypen
            )?.let {
                ZaaktypeCompletionParameters().apply {
                    zaakbeeindigReden = zaakbeeindigParameter.zaakbeeindigReden
                    resultaattype = it
                }
            }
        }
    }

    private fun mapPreviousResultaattypeToNewResultaattype(
        previousResultaattypeUUID: UUID,
        newResultaattypen: List<ResultaatType>,
    ): UUID? =
        ztcClientService.readResultaattype(previousResultaattypeUUID)
            .let { newResultaattypen.firstOrNull { it.omschrijving == it.omschrijving } }
            ?.url
            ?.extractUuid()

    /**
     * Copying of the ZaakbeeindigGegevens from the old ZaaktypeCmmnConfiguration to the new ZaaktypeCmmnConfiguration
     *
     * @param previousZaaktypeCmmnConfiguration source
     * @param newZaaktypeCmmnConfiguration      destination
     * @param newZaaktype                       new zaaktype to read the results from
     */
    fun mapZaakbeeindigGegevens(
        previousZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        newZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        newZaaktype: ZaakType
    ) {
        val newResultaattypen = newZaaktype.resultaattypen.map { ztcClientService.readResultaattype(it) }
        newZaaktypeCmmnConfiguration.nietOntvankelijkResultaattype =
            previousZaaktypeCmmnConfiguration.nietOntvankelijkResultaattype?.let {
                mapPreviousResultaattypeToNewResultaattype(it, newResultaattypen)
            }
        val zaakbeeindigParametersCollection = previousZaaktypeCmmnConfiguration.getZaakbeeindigParameters()
            .mapNotNull { zaakbeeindigParameter ->
                zaakbeeindigParameter.resultaattype
                    .let { mapPreviousResultaattypeToNewResultaattype(it, newResultaattypen) }
                    ?.let {
                        ZaaktypeCompletionParameters().apply {
                            zaakbeeindigReden = zaakbeeindigParameter.zaakbeeindigReden
                            resultaattype = it
                        }
                    }
            }.toMutableSet()
        newZaaktypeCmmnConfiguration.setZaakbeeindigParameters(zaakbeeindigParametersCollection)
    }
}
