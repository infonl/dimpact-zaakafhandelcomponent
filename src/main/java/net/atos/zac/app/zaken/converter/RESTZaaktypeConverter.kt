/*
 * SPDX-FileCopyrightText: 2021 Atos
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
        val restZaaktype = RESTZaaktype()
        restZaaktype.uuid = UriUtil.uuidFromURI(zaaktype.url)
        restZaaktype.identificatie = zaaktype.identificatie
        restZaaktype.doel = zaaktype.doel
        restZaaktype.omschrijving = zaaktype.omschrijving
        restZaaktype.servicenorm = isServicenormBeschikbaar(zaaktype)
        restZaaktype.versiedatum = zaaktype.versiedatum
        restZaaktype.nuGeldig = isNuGeldig(zaaktype)
        restZaaktype.beginGeldigheid = zaaktype.beginGeldigheid
        restZaaktype.eindeGeldigheid = zaaktype.eindeGeldigheid
        restZaaktype.vertrouwelijkheidaanduiding = zaaktype.vertrouwelijkheidaanduiding
        restZaaktype.opschortingMogelijk = zaaktype.opschortingEnAanhoudingMogelijk
        restZaaktype.verlengingMogelijk = zaaktype.verlengingMogelijk
        if (restZaaktype.verlengingMogelijk) {
            restZaaktype.verlengingstermijn = PeriodUtil.aantalDagenVanafHeden(
                Period.parse(zaaktype.verlengingstermijn)
            )
        }
        restZaaktype.zaaktypeRelaties = ArrayList()
        zaaktype.deelzaaktypen?.let {
            zaaktype.deelzaaktypen.stream()
                .map { deelzaaktype ->
                    convertToRESTZaaktypeRelatie(
                        deelzaaktype,
                        RelatieType.DEELZAAK
                    )
                }
                .forEach { restZaaktypeRelatie ->
                    (restZaaktype.zaaktypeRelaties as ArrayList<RESTZaaktypeRelatie>).add(
                        restZaaktypeRelatie
                    )
                }
        }
        if (zaaktype.gerelateerdeZaaktypen != null) {
            zaaktype.gerelateerdeZaaktypen.stream()
                .map { zaakTypenRelatie -> convertToRESTZaaktypeRelatie(zaakTypenRelatie) }
                .forEach {
                        restZaaktypeRelatie ->
                    (restZaaktype.zaaktypeRelaties as ArrayList<RESTZaaktypeRelatie>).add(restZaaktypeRelatie)
                }
        }
        restZaaktype.informatieobjecttypes = zaaktype.informatieobjecttypen.stream().map { uri -> UriUtil.uuidFromURI(uri) }.toList()
        if (zaaktype.referentieproces != null) {
            restZaaktype.referentieproces = zaaktype.referentieproces.naam
        }
        val zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(
            restZaaktype.uuid
        )
        restZaaktype.zaakafhandelparameters = zaakafhandelParametersConverter.convertZaakafhandelParameters(
            zaakafhandelParameters, true
        )
        return restZaaktype
    }
}
