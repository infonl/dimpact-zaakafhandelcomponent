/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.converter

import jakarta.inject.Inject
import net.atos.client.zgw.ztc.model.generated.ZaakType
import net.atos.client.zgw.ztc.util.isNuGeldig
import net.atos.client.zgw.ztc.util.isServicenormBeschikbaar
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.app.admin.converter.RestZaakafhandelParametersConverter
import net.atos.zac.app.zaak.model.RelatieType
import net.atos.zac.app.zaak.model.RestZaaktype
import net.atos.zac.app.zaak.model.RestZaaktypeRelatie
import net.atos.zac.app.zaak.model.toRestZaaktypeRelatie
import net.atos.zac.util.PeriodUtil
import net.atos.zac.util.UriUtil
import java.time.Period

class RestZaaktypeConverter @Inject constructor(
    private val zaakafhandelParametersConverter: RestZaakafhandelParametersConverter,
    private val zaakafhandelParameterService: ZaakafhandelParameterService
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
            uuid = UriUtil.uuidFromURI(zaaktype.url),
            identificatie = zaaktype.identificatie,
            doel = zaaktype.doel,
            omschrijving = zaaktype.omschrijving,
            servicenorm = isServicenormBeschikbaar(zaaktype),
            versiedatum = zaaktype.versiedatum,
            nuGeldig = isNuGeldig(zaaktype),
            beginGeldigheid = zaaktype.beginGeldigheid,
            eindeGeldigheid = zaaktype.eindeGeldigheid,
            vertrouwelijkheidaanduiding = zaaktype.vertrouwelijkheidaanduiding,
            opschortingMogelijk = zaaktype.opschortingEnAanhoudingMogelijk,
            verlengingMogelijk = zaaktype.verlengingMogelijk,
            verlengingstermijn = if (zaaktype.verlengingMogelijk) {
                PeriodUtil.aantalDagenVanafHeden(
                    Period.parse(zaaktype.verlengingstermijn)
                )
            } else {
                null
            },
            zaaktypeRelaties = zaaktypeRelaties,
            informatieobjecttypes = zaaktype.informatieobjecttypen.stream().map { uri ->
                UriUtil.uuidFromURI(uri)
            }.toList(),
            referentieproces = zaaktype.referentieproces?.naam,
            zaakafhandelparameters = zaakafhandelParametersConverter.toRestZaakafhandelParameters(
                zaakafhandelParameterService.readZaakafhandelParameters(UriUtil.uuidFromURI(zaaktype.url)),
                true
            )
        )
    }
}
