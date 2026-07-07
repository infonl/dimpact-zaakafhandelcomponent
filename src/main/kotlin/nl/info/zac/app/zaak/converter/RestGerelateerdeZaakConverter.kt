/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.converter

import jakarta.inject.Inject
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.GerelateerdeZaak
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.app.policy.model.toRestZaakRechten
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.app.zaak.model.RestGerelateerdeZaak
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.output.ZaakRechten
import nl.info.zac.zaak.hoofdAndDeelzaakCanBeOntkoppeld
import nl.info.zac.zaak.model.toZaakLinkData
import nl.info.zac.zaak.relatedZakenCanBeOntkoppeld

class RestGerelateerdeZaakConverter @Inject constructor(
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService,
    private val policyService: PolicyService
) {
    fun convert(
        fromZaak: Zaak,
        fromZaakRechten: ZaakRechten,
        gerelateerdeZaak: Zaak,
        loggedInUser: LoggedInUser,
        relatieType: RelatieType?
    ): RestGerelateerdeZaak {
        val zaaktype = ztcClientService.readZaaktype(gerelateerdeZaak.zaaktype)
        val zaakrechten = policyService.readZaakRechten(gerelateerdeZaak, zaaktype, loggedInUser)
        return RestGerelateerdeZaak(
            identificatie = gerelateerdeZaak.identificatie,
            relatieType = relatieType,
            rechten = zaakrechten.toRestZaakRechten(),
            zaaktypeOmschrijving = takeIf { zaakrechten.lezen }?.let { zaaktype.omschrijving },
            startdatum = takeIf { zaakrechten.lezen }?.let { gerelateerdeZaak.startdatum },
            statustypeOmschrijving = takeIf { zaakrechten.lezen }?.let {
                gerelateerdeZaak.status?.let {
                    zrcClientService.readStatus(it).let { zaakstatus ->
                        ztcClientService.readStatustype(zaakstatus.statustype).omschrijving
                    }
                }
            },
            ontkoppelen = when (relatieType) {
                RelatieType.GERELATEERD -> relatedZakenCanBeOntkoppeld(
                    fromZaak.toZaakLinkData(fromZaakRechten),
                    gerelateerdeZaak.toZaakLinkData(zaakrechten)
                )
                RelatieType.HOOFDZAAK -> hoofdAndDeelzaakCanBeOntkoppeld(
                    gerelateerdeZaak.toZaakLinkData(zaakrechten),
                    fromZaak.toZaakLinkData(fromZaakRechten)
                )
                RelatieType.DEELZAAK -> hoofdAndDeelzaakCanBeOntkoppeld(
                    fromZaak.toZaakLinkData(fromZaakRechten),
                    gerelateerdeZaak.toZaakLinkData(zaakrechten)
                )
                else -> false
            }
        )
    }

    fun convert(
        fromZaak: Zaak,
        fromZaakRechten: ZaakRechten,
        gerelateerdeZaak: GerelateerdeZaak,
        loggedInUser: LoggedInUser
    ): RestGerelateerdeZaak {
        val zaak = zrcClientService.readZaak(gerelateerdeZaak.url)
        return convert(
            fromZaak = fromZaak,
            fromZaakRechten = fromZaakRechten,
            gerelateerdeZaak = zaak,
            loggedInUser = loggedInUser,
            relatieType = RelatieType.GERELATEERD
        )
    }
}
