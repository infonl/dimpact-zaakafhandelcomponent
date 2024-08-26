/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.converter

import jakarta.inject.Inject
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.AardRelatie
import net.atos.client.zgw.zrc.model.RelevanteZaak
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.app.policy.converter.RESTRechtenConverter
import net.atos.zac.app.zaak.model.RelatieType
import net.atos.zac.app.zaak.model.RestGerelateerdeZaak
import net.atos.zac.policy.PolicyService

class RESTGerelateerdeZaakConverter {
    @Inject
    private lateinit var zrcClientService: ZrcClientService

    @Inject
    private lateinit var ztcClientService: ZtcClientService

    @Inject
    private lateinit var rechtenConverter: RESTRechtenConverter

    @Inject
    private lateinit var policyService: PolicyService

    fun convert(zaak: Zaak, relatieType: RelatieType?): RestGerelateerdeZaak {
        val zaaktype = ztcClientService.readZaaktype(zaak.zaaktype)
        val zaakrechten = policyService.readZaakRechten(zaak, zaaktype)
        val restGerelateerdeZaak = RestGerelateerdeZaak()
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

    fun convert(relevanteZaak: RelevanteZaak): RestGerelateerdeZaak {
        val zaak = zrcClientService.readZaak(relevanteZaak.url)
        return convert(zaak, convertToRelatieType(relevanteZaak.aardRelatie))
    }

    fun convertToRelatieType(aardRelatie: AardRelatie) = when (aardRelatie) {
        AardRelatie.VERVOLG -> RelatieType.VERVOLG
        AardRelatie.BIJDRAGE -> RelatieType.BIJDRAGE
        AardRelatie.ONDERWERP -> RelatieType.ONDERWERP
    }
}
