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
import nl.info.zac.zaak.model.canBeUnlinkedFromDeelzaak
import nl.info.zac.zaak.model.canBeUnlinkedFromRelatedZaak
import nl.info.zac.zaak.model.toZaakLinkData

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
        val zaakrechten = policyService.readZaakRechten(
            zaak = gerelateerdeZaak,
            zaaktype = zaaktype,
            loggedInUser = loggedInUser
        )
        return RestGerelateerdeZaak(
            identificatie = gerelateerdeZaak.identificatie,
            relatieType = relatieType,
            rechten = zaakrechten.toRestZaakRechten(),
            zaaktypeOmschrijving = takeIf { zaakrechten.canLezen }?.let { zaaktype.omschrijving },
            startdatum = takeIf { zaakrechten.canLezen }?.let { gerelateerdeZaak.startdatum },
            statustypeOmschrijving = takeIf { zaakrechten.canLezen }?.let {
                gerelateerdeZaak.status?.let { statusURI ->
                    zrcClientService.readStatus(statusURI).let { zaakstatus ->
                        ztcClientService.readStatustype(zaakstatus.statustype).omschrijving
                    }
                }
            },
            canOntkoppelen = when (relatieType) {
                RelatieType.GERELATEERD -> fromZaak
                    .toZaakLinkData(fromZaakRechten)
                    .canBeUnlinkedFromRelatedZaak(
                        gerelateerdeZaak.toZaakLinkData(zaakrechten)
                    )
                RelatieType.HOOFDZAAK -> gerelateerdeZaak
                    .toZaakLinkData(zaakrechten)
                    .canBeUnlinkedFromDeelzaak(
                        fromZaak.toZaakLinkData(fromZaakRechten)
                    )
                RelatieType.DEELZAAK -> fromZaak.toZaakLinkData(fromZaakRechten)
                    .canBeUnlinkedFromDeelzaak(
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
