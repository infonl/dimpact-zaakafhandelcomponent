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
     * Pas de ZaakbeeindigGegevens aan op basis van het gegeven zaaktype
     *
     * @param zaaktypeConfiguration bron
     * @param newZaaktype           het zaaktype om de resultaten van te lezen
     */
    fun updateZaakbeeindigGegevens(
        zaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktype: ZaakType
    ) {
        val newResultaattypen = newZaaktype.resultaattypen.map { ztcClientService.readResultaattype(it) }

        zaaktypeConfiguration.nietOntvankelijkResultaattype?.let {
            mapVorigResultaattypeOpNieuwResultaattype(it, newResultaattypen)
        }

        zaaktypeConfiguration.getZaakbeeindigParameters().mapNotNull { zaakbeeindigParameter ->
            mapVorigResultaattypeOpNieuwResultaattype(
                zaakbeeindigParameter.resultaattype,
                newResultaattypen
            )
                ?.let {
                    ZaaktypeCompletionParameters().apply {
                        zaakbeeindigReden = zaakbeeindigParameter.zaakbeeindigReden
                        resultaattype = it
                    }
                }
        }
    }

    private fun mapVorigResultaattypeOpNieuwResultaattype(
        previousResultaattypeUUID: UUID,
        newResultaattypen: List<ResultaatType>,
    ): UUID? =
        ztcClientService.readResultaattype(previousResultaattypeUUID)
            .let { newResultaattypen.firstOrNull { it.omschrijving == it.omschrijving } }
            ?.url
            ?.extractUuid()

    /**
     * Kopieren van de ZaakbeeindigGegevens van de oude ZaaktypeCmmnConfiguration naar de nieuw ZaaktypeCmmnConfiguration
     *
     * @param previousZaaktypeCmmnConfiguration bron
     * @param newZaaktypeCmmnConfiguration bestemming
     * @param newZaaktype                het nieuwe zaaktype om de resultaten van te lezen
     */
    fun mapZaakbeeindigGegevens(
        previousZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        newZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        newZaaktype: ZaakType
    ) {
        val newResultaattypen = newZaaktype.resultaattypen.map { ztcClientService.readResultaattype(it) }
        newZaaktypeCmmnConfiguration.nietOntvankelijkResultaattype =
            previousZaaktypeCmmnConfiguration.nietOntvankelijkResultaattype?.let {
                mapVorigResultaattypeOpNieuwResultaattype(it, newResultaattypen)
            }
        val zaakbeeindigParametersCollection = previousZaaktypeCmmnConfiguration.getZaakbeeindigParameters()
            .mapNotNull { zaakbeeindigParameter ->
                zaakbeeindigParameter.resultaattype
                    .let { mapVorigResultaattypeOpNieuwResultaattype(it, newResultaattypen) }
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
