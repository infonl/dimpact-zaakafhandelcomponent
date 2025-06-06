/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.converter

import jakarta.inject.Inject
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.AardRelatieEnum
import nl.info.client.zgw.zrc.model.generated.RelevanteZaak
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.app.policy.model.toRestZaakRechten
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.app.zaak.model.RestGerelateerdeZaak
import nl.info.zac.policy.PolicyService

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
            rechten = zaakrechten.toRestZaakRechten(),
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

    fun convertToRelatieType(aardRelatie: AardRelatieEnum) = when (aardRelatie) {
        AardRelatieEnum.VERVOLG -> RelatieType.VERVOLG
        AardRelatieEnum.BIJDRAGE -> RelatieType.BIJDRAGE
        AardRelatieEnum.ONDERWERP -> RelatieType.ONDERWERP
        AardRelatieEnum.OVERIG -> RelatieType.OVERIG
    }
}
