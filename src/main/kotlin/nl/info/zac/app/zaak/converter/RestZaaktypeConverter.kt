/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.converter

import jakarta.inject.Inject
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import net.atos.zac.util.time.PeriodUtil
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.model.extensions.isNuGeldig
import nl.info.client.zgw.ztc.model.extensions.isServicenormAvailable
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.app.admin.converter.RestZaakafhandelParametersConverter
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.app.zaak.model.RestZaaktype
import nl.info.zac.app.zaak.model.RestZaaktypeRelatie
import nl.info.zac.app.zaak.model.toRestZaaktypeRelatie
import java.time.Period

class RestZaaktypeConverter @Inject constructor(
    private val zaakafhandelParametersConverter: RestZaakafhandelParametersConverter,
    private val zaaktypeCmmnConfigurationService: ZaaktypeCmmnConfigurationService
) {
    fun convert(zaaktype: ZaakType): RestZaaktype {
        val zaaktypeRelaties = ArrayList<RestZaaktypeRelatie>()
        zaaktype.deelzaaktypen?.let { deelzaakType ->
            deelzaakType
                .map { RelatieType.DEELZAAK.toRestZaaktypeRelatie(it) }
                .forEach { zaaktypeRelaties.add(it) }
        }
        zaaktype.gerelateerdeZaaktypen?.let { gerelateerdeZaaktypen ->
            gerelateerdeZaaktypen
                .map { it.toRestZaaktypeRelatie() }
                .forEach { zaaktypeRelaties.add(it) }
        }

        return RestZaaktype(
            uuid = zaaktype.url.extractUuid(),
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
            verlengingstermijn = if (zaaktype.verlengingMogelijk) {
                PeriodUtil.numberOfDaysFromToday(
                    Period.parse(zaaktype.verlengingstermijn)
                )
            } else {
                null
            },
            zaaktypeRelaties = zaaktypeRelaties,
            informatieobjecttypes = zaaktype.informatieobjecttypen.map { it.extractUuid() },
            referentieproces = zaaktype.referentieproces?.naam,
            zaakafhandelparameters = zaakafhandelParametersConverter.toRestZaaktypeCmmnConfiguration(
                zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktype.url.extractUuid()),
                true
            )
        )
    }
}
