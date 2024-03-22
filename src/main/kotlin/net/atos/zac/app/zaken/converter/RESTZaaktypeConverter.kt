/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.converter

import jakarta.inject.Inject
import net.atos.client.zgw.ztc.model.generated.ZaakType
import net.atos.client.zgw.ztc.util.isNuGeldig
import net.atos.client.zgw.ztc.util.isServicenormBeschikbaar
import net.atos.zac.app.admin.converter.RESTZaakafhandelParametersConverter
import net.atos.zac.app.zaken.model.RESTZaaktype
import net.atos.zac.app.zaken.model.RESTZaaktypeRelatie
import net.atos.zac.app.zaken.model.RelatieType
import net.atos.zac.util.PeriodUtil
import net.atos.zac.util.UriUtil
import net.atos.zac.zaaksturing.ZaakafhandelParameterService
import java.time.Period

class RESTZaaktypeConverter {
    @Inject
    private lateinit var zaakafhandelParametersConverter: RESTZaakafhandelParametersConverter

    @Inject
    private lateinit var zaakafhandelParameterService: ZaakafhandelParameterService

    fun convert(zaaktype: ZaakType): RESTZaaktype {
        val zaaktypeRelaties = ArrayList<RESTZaaktypeRelatie>()
        zaaktype.deelzaaktypen?.let { deelzaaktypen ->
            deelzaaktypen.stream()
                .map { deelzaaktype ->
                    convertToRESTZaaktypeRelatie(
                        deelzaaktype,
                        RelatieType.DEELZAAK
                    )
                }
                .forEach { restZaaktypeRelatie -> zaaktypeRelaties.add(restZaaktypeRelatie) }
        }
        zaaktype.gerelateerdeZaaktypen?.let { gerelateerdeZaaktypen ->
            gerelateerdeZaaktypen.stream()
                .map { zaakTypenRelatie -> convertToRESTZaaktypeRelatie(zaakTypenRelatie) }
                .forEach {
                        restZaaktypeRelatie ->
                    zaaktypeRelaties.add(restZaaktypeRelatie)
                }
        }

        return RESTZaaktype(
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
            informatieobjecttypes = zaaktype.informatieobjecttypen.stream().map {
                    uri ->
                UriUtil.uuidFromURI(uri)
            }.toList(),
            referentieproces = zaaktype.referentieproces?.let { zaaktype.referentieproces.naam },
            zaakafhandelparameters = zaakafhandelParametersConverter.convertZaakafhandelParameters(
                zaakafhandelParameterService.readZaakafhandelParameters(UriUtil.uuidFromURI(zaaktype.url)),
                true
            )
        )
    }
}
