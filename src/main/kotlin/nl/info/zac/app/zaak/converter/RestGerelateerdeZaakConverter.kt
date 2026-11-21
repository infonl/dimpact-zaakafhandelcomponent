/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.converter

import jakarta.inject.Inject
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.AardRelatie
import net.atos.client.zgw.zrc.model.RelevanteZaak
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.app.policy.converter.RestRechtenConverter
import net.atos.zac.policy.PolicyService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.app.zaak.model.RestGerelateerdeZaak

class RestGerelateerdeZaakConverter @Inject constructor(
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService,
    private val policyService: PolicyService
) {
    fun convert(zaak: Zaak, relatieType: RelatieType?): RestGerelateerdeZaak {
        val zaaktype = ztcClientService.readZaaktype(zaak.zaaktype)
        val zaakrechten = policyService.readZaakRechten(zaak, zaaktype)
        return RestGerelateerdeZaak(
            identificatie = zaak.identificatie,
            relatieType = relatieType,
            rechten = RestRechtenConverter.convert(zaakrechten),
            zaaktypeOmschrijving = takeIf { zaakrechten.lezen }?.let { zaaktype.omschrijving },
            startdatum = takeIf { zaakrechten.lezen }?.let { zaak.startdatum },
            statustypeOmschrijving = takeIf { zaakrechten.lezen }?.let {
                zaak.status?.let {
                    zrcClientService.readStatus(it).let { zaakstatus ->
                        ztcClientService.readStatustype(zaakstatus.statustype).omschrijving
                    }
                }
            }
        )
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
