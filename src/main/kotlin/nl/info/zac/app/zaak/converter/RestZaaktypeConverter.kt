/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.converter

import jakarta.inject.Inject
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.model.extensions.extensionPeriodDays
import nl.info.client.zgw.ztc.model.extensions.isNuGeldig
import nl.info.client.zgw.ztc.model.extensions.isServicenormAvailable
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.admin.ZaaktypeBpmnConfigurationBeheerService
import nl.info.zac.app.admin.converter.RestZaakafhandelParametersConverter
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.app.zaak.model.RestZaaktype
import nl.info.zac.app.zaak.model.RestZaaktypeRelatie
import nl.info.zac.app.zaak.model.toRestZaaktypeRelatie

class RestZaaktypeConverter @Inject constructor(
    private val zaakafhandelParametersConverter: RestZaakafhandelParametersConverter,
    private val zaaktypeCmmnConfigurationService: ZaaktypeCmmnConfigurationService,
    private val zaaktypeBpmnConfigurationBeheerService: ZaaktypeBpmnConfigurationBeheerService
) {
    fun convert(zaaktype: ZaakType): RestZaaktype {
        val zaaktypeUuid = zaaktype.url.extractUuid()
        val zaaktypeRelaties = buildList<RestZaaktypeRelatie> {
            zaaktype.deelzaaktypen?.forEach { add(RelatieType.DEELZAAK.toRestZaaktypeRelatie(it)) }
            zaaktype.gerelateerdeZaaktypen?.forEach { add(it.toRestZaaktypeRelatie()) }
        }
        return RestZaaktype(
            uuid = zaaktypeUuid,
            identificatie = zaaktype.identificatie,
            doel = zaaktype.doel,
            omschrijving = zaaktype.omschrijving,
            servicenorm = zaaktype.isServicenormAvailable(),
            versiedatum = zaaktype.versiedatum,
            nuGeldig = zaaktype.isNuGeldig(),
            beginGeldigheid = zaaktype.beginGeldigheid,
            eindeGeldigheid = zaaktype.eindeGeldigheid,
            vertrouwelijkheidaanduiding = zaaktype.vertrouwelijkheidaanduiding,
            opschortingMogelijk = zaaktype.opschortingEnAanhoudingMogelijk,
            verlengingMogelijk = zaaktype.verlengingMogelijk,
            verlengingstermijn = zaaktype.extensionPeriodDays(),
            zaaktypeRelaties = zaaktypeRelaties,
            informatieobjecttypes = zaaktype.informatieobjecttypen.map { it.extractUuid() },
            referentieproces = zaaktype.referentieproces?.naam,
            zaakafhandelparameters = zaaktypeUuid.let { uuid ->
                zaaktypeBpmnConfigurationBeheerService.findConfiguration(uuid)?.let {
                    zaakafhandelParametersConverter.toRestZaakafhandelParameters(it)
                } ?: zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(uuid).let {
                    zaakafhandelParametersConverter.toRestZaakafhandelParameters(it, true)
                }
            }
        )
    }
}
