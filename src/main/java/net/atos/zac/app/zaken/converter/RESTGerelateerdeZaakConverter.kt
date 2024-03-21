/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.converter

import jakarta.inject.Inject
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.AardRelatie
import net.atos.client.zgw.zrc.model.RelevanteZaak
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.zac.app.policy.converter.RESTRechtenConverter
import net.atos.zac.app.zaken.model.RESTGerelateerdeZaak
import net.atos.zac.app.zaken.model.RelatieType
import net.atos.zac.policy.PolicyService

class RESTGerelateerdeZaakConverter {
    @Inject
    private lateinit var zrcClientService: ZRCClientService

    @Inject
    private lateinit var ztcClientService: ZTCClientService

    @Inject
    private lateinit var rechtenConverter: RESTRechtenConverter

    @Inject
    private lateinit var policyService: PolicyService

    fun convert(zaak: Zaak, relatieType: RelatieType?): RESTGerelateerdeZaak {
        val zaaktype = ztcClientService.readZaaktype(zaak.zaaktype)
        val zaakrechten = policyService.readZaakRechten(zaak, zaaktype)
        val restGerelateerdeZaak = RESTGerelateerdeZaak()
        restGerelateerdeZaak.identificatie = zaak.identificatie
        restGerelateerdeZaak.relatieType = relatieType
        restGerelateerdeZaak.rechten = rechtenConverter.convert(zaakrechten)
        if (zaakrechten.lezen) {
            restGerelateerdeZaak.zaaktypeOmschrijving = zaaktype.omschrijving
            restGerelateerdeZaak.startdatum = zaak.startdatum
            if (zaak.status != null) {
                restGerelateerdeZaak.statustypeOmschrijving = ztcClientService.readStatustype(
                    zrcClientService.readStatus(zaak.status).statustype
                ).omschrijving
            }
        }
        return restGerelateerdeZaak
    }

    fun convert(relevanteZaak: RelevanteZaak): RESTGerelateerdeZaak {
        val zaak = zrcClientService.readZaak(relevanteZaak.url)
        return convert(zaak, convertToRelatieType(relevanteZaak.aardRelatie))
    }

    fun convertToRelatieType(aardRelatie: AardRelatie) = when (aardRelatie) {
        AardRelatie.VERVOLG -> RelatieType.VERVOLG
        AardRelatie.BIJDRAGE -> RelatieType.BIJDRAGE
        AardRelatie.ONDERWERP -> RelatieType.ONDERWERP
    }
}
