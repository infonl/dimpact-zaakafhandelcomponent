/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.converter

import jakarta.inject.Inject
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.zac.app.identity.converter.RESTGroupConverter
import net.atos.zac.app.identity.converter.RESTUserConverter
import net.atos.zac.app.policy.converter.RESTRechtenConverter
import net.atos.zac.app.zaken.model.RESTZaakOverzicht
import net.atos.zac.policy.PolicyService

class RESTZaakOverzichtConverter {
    @Inject
    private lateinit var ztcClientService: ZTCClientService

    @Inject
    private lateinit var zgwApiService: ZGWApiService

    @Inject
    private lateinit var zaakResultaatConverter: RESTZaakResultaatConverter

    @Inject
    private lateinit var groupConverter: RESTGroupConverter

    @Inject
    private lateinit var userConverter: RESTUserConverter

    @Inject
    private lateinit var openstaandeTakenConverter: RESTOpenstaandeTakenConverter

    @Inject
    private lateinit var rechtenConverter: RESTRechtenConverter

    @Inject
    private lateinit var policyService: PolicyService

    @Inject
    private lateinit var zrcClientService: ZRCClientService

    fun convert(zaak: Zaak): RESTZaakOverzicht {
        val zaaktype = ztcClientService.readZaaktype(zaak.zaaktype)
        val zaakrechten = policyService.readZaakRechten(zaak, zaaktype)
        val restZaakOverzicht = RESTZaakOverzicht()
        restZaakOverzicht.uuid = zaak.uuid
        restZaakOverzicht.identificatie = zaak.identificatie
        restZaakOverzicht.rechten = rechtenConverter.convert(zaakrechten)
        if (zaakrechten.lezen) {
            restZaakOverzicht.startdatum = zaak.startdatum
            restZaakOverzicht.einddatum = zaak.einddatum
            restZaakOverzicht.einddatumGepland = zaak.einddatumGepland
            restZaakOverzicht.uiterlijkeEinddatumAfdoening = zaak.uiterlijkeEinddatumAfdoening
            restZaakOverzicht.toelichting = zaak.toelichting
            restZaakOverzicht.omschrijving = zaak.omschrijving
            restZaakOverzicht.zaaktype = zaaktype.omschrijving
            restZaakOverzicht.openstaandeTaken = openstaandeTakenConverter.convert(zaak.uuid)
            restZaakOverzicht.resultaat = zaak.resultaat?.let { resultaat ->
                zaakResultaatConverter.convert(resultaat)
            }
            zaak.status?.let {
                restZaakOverzicht.status = ztcClientService.readStatustype(
                    zrcClientService.readStatus(zaak.status).statustype
                ).omschrijving
            }
            zgwApiService.findBehandelaarForZaak(zaak)
                .map { behandelaar ->
                    userConverter.convertUserId(behandelaar.betrokkeneIdentificatie.identificatie)
                }
                .ifPresent { behandelaar -> restZaakOverzicht.behandelaar = behandelaar }
            zgwApiService.findGroepForZaak(zaak)
                .map { groep -> groupConverter.convertGroupId(groep.betrokkeneIdentificatie.identificatie) }
                .ifPresent { groep -> restZaakOverzicht.groep = groep }
        }
        return restZaakOverzicht
    }
}
