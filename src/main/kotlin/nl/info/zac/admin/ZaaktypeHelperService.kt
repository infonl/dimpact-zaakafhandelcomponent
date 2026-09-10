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
import nl.info.zac.admin.model.ZaaktypeBetrokkeneParameters
import nl.info.zac.admin.model.ZaaktypeBrpParameters
import nl.info.zac.admin.model.ZaaktypeCompletionParameters
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime
import java.util.UUID

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
class ZaaktypeHelperService @Inject constructor(
    private val ztcClientService: ZtcClientService,
) {
    fun copySharedConfigurationData(
        previousZaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktype: ZaakType
    ) {
        newZaaktypeConfiguration.apply {
            groepID = previousZaaktypeConfiguration.groepID
            defaultBehandelaarId = previousZaaktypeConfiguration.defaultBehandelaarId
            productaanvraagtype = previousZaaktypeConfiguration.productaanvraagtype
            smartDocumentsEnabled = previousZaaktypeConfiguration.smartDocumentsEnabled
            creatiedatum = ZonedDateTime.now()
        }
        copyBetrokkeneKoppelingen(previousZaaktypeConfiguration, newZaaktypeConfiguration)
        copyBrpDoelbindingen(previousZaaktypeConfiguration, newZaaktypeConfiguration)
        mapZaakbeeindigGegevens(previousZaaktypeConfiguration, newZaaktypeConfiguration, newZaaktype)
    }

    private fun copyBetrokkeneKoppelingen(
        previousZaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktypeConfiguration: ZaaktypeConfiguration
    ) {
        newZaaktypeConfiguration.zaaktypeBetrokkeneParameters = ZaaktypeBetrokkeneParameters().apply {
            zaaktypeConfiguration = newZaaktypeConfiguration
            brpKoppelen = previousZaaktypeConfiguration.zaaktypeBetrokkeneParameters?.brpKoppelen
            kvkKoppelen = previousZaaktypeConfiguration.zaaktypeBetrokkeneParameters?.kvkKoppelen
        }
    }

    private fun copyBrpDoelbindingen(
        previousZaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktypeConfiguration: ZaaktypeConfiguration
    ) {
        newZaaktypeConfiguration.zaaktypeBrpParameters = ZaaktypeBrpParameters().apply {
            zaaktypeConfiguration = newZaaktypeConfiguration
            zoekWaarde = previousZaaktypeConfiguration.zaaktypeBrpParameters?.zoekWaarde
            raadpleegWaarde = previousZaaktypeConfiguration.zaaktypeBrpParameters?.raadpleegWaarde
            verwerkingregisterWaarde = previousZaaktypeConfiguration.zaaktypeBrpParameters?.verwerkingregisterWaarde
        }
    }

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
     * Source and destination may be the same instance; everything is read into local variables before the first write.
     */
    fun mapZaakbeeindigGegevens(
        previousZaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktypeConfiguration: ZaaktypeConfiguration,
        newZaaktype: ZaakType
    ) {
        val newResultaattypen = newZaaktype.resultaattypen.map { ztcClientService.readResultaattype(it) }
        val nietOntvankelijkResultaattype = previousZaaktypeConfiguration.nietOntvankelijkResultaattype?.let {
            mapPreviousResultaattypeToNewResultaattype(it, newResultaattypen)
        }
        val zaakbeeindigParametersCollection = previousZaaktypeConfiguration.getZaakbeeindigParameters()
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
        newZaaktypeConfiguration.nietOntvankelijkResultaattype = nietOntvankelijkResultaattype
        newZaaktypeConfiguration.setZaakbeeindigParameters(zaakbeeindigParametersCollection)
    }
}
