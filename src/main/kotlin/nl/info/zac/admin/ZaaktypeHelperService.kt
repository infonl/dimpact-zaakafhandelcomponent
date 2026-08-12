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
     * Remaps the ZaakbeeindigGegevens of the given zaaktype configuration onto the resultaattypen of the given
     * zaaktype, in place. Passing the configuration as both source and destination is safe: [mapZaakbeeindigGegevens]
     * resolves all resultaattypen into local variables before it writes anything back.
     *
     * @param zaaktypeConfiguration source and destination
     * @param newZaaktype           zaaktype to read the results from
     */
    fun updateZaakbeeindigGegevens(
        zaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktype: ZaakType
    ) = mapZaakbeeindigGegevens(zaaktypeConfiguration, zaaktypeConfiguration, newZaaktype)

    private fun mapPreviousResultaattypeToNewResultaattype(
        previousResultaattypeUUID: UUID,
        newResultaattypen: List<ResultaatType>,
    ): UUID? =
        ztcClientService.readResultaattype(previousResultaattypeUUID).let { previousResultaattype ->
            newResultaattypen.firstOrNull { it.omschrijving == previousResultaattype.omschrijving }
        }
            ?.url
            ?.extractUuid()

    /**
     * Copying of the ZaakbeeindigGegevens from the old ZaaktypeConfiguration to the new ZaaktypeConfiguration.
     * Resultaattypen of the previous configuration are matched to those of the new zaaktype by omschrijving;
     * parameters without a match are dropped.
     *
     * Source and destination may be the same instance; everything is read into local variables before the first write.
     *
     * @param previousZaaktypeCmmnConfiguration source
     * @param newZaaktypeCmmnConfiguration      destination
     * @param newZaaktype                       new zaaktype to read the results from
     */
    fun mapZaakbeeindigGegevens(
        previousZaaktypeCmmnConfiguration: ZaaktypeConfiguration,
        newZaaktypeCmmnConfiguration: ZaaktypeConfiguration,
        newZaaktype: ZaakType
    ) {
        val newResultaattypen = newZaaktype.resultaattypen.map { ztcClientService.readResultaattype(it) }
        val nietOntvankelijkResultaattype = previousZaaktypeCmmnConfiguration.nietOntvankelijkResultaattype?.let {
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
        newZaaktypeCmmnConfiguration.nietOntvankelijkResultaattype = nietOntvankelijkResultaattype
        newZaaktypeCmmnConfiguration.setZaakbeeindigParameters(zaakbeeindigParametersCollection)
    }
}
