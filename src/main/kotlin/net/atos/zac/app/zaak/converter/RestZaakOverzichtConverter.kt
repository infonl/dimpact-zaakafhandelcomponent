/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.converter

import jakarta.inject.Inject
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.app.identity.converter.RestGroupConverter
import net.atos.zac.app.identity.converter.RestUserConverter
import net.atos.zac.app.policy.converter.RestRechtenConverter
import net.atos.zac.app.zaak.model.RestZaakOverzicht
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.policy.PolicyService

@Suppress("LongParameterList")
class RestZaakOverzichtConverter @Inject constructor(
    private val ztcClientService: ZtcClientService,
    private val zgwApiService: ZGWApiService,
    private val zaakResultaatConverter: RestZaakResultaatConverter,
    private val groupConverter: RestGroupConverter,
    private val userConverter: RestUserConverter,
    private val openstaandeTakenConverter: RestOpenstaandeTakenConverter,
    private val rechtenConverter: RestRechtenConverter,
    private val policyService: PolicyService,
    private val zrcClientService: ZrcClientService,
) {
    fun convert(zaak: Zaak, user: LoggedInUser? = null): RestZaakOverzicht {
        val zaaktype = ztcClientService.readZaaktype(zaak.zaaktype)
        val zaakrechten = policyService.readZaakRechten(zaak, zaaktype, user)
        return RestZaakOverzicht(
            uuid = zaak.uuid,
            identificatie = zaak.identificatie,
            rechten = rechtenConverter.convert(zaakrechten),
            startdatum = takeIf { zaakrechten.lezen }?.let { zaak.startdatum },
            einddatum = takeIf { zaakrechten.lezen }?.let { zaak.einddatum },
            einddatumGepland = takeIf { zaakrechten.lezen }?.let { zaak.einddatumGepland },
            uiterlijkeEinddatumAfdoening = takeIf { zaakrechten.lezen }?.let { zaak.uiterlijkeEinddatumAfdoening },
            toelichting = takeIf { zaakrechten.lezen }?.let { zaak.toelichting },
            omschrijving = takeIf { zaakrechten.lezen }?.let { zaak.omschrijving },
            zaaktype = takeIf { zaakrechten.lezen }?.let { zaaktype.omschrijving },
            openstaandeTaken = openstaandeTakenConverter.convert(zaak.uuid),
            resultaat = takeIf { zaakrechten.lezen }?.let {
                zaak.resultaat?.let {
                    zaakResultaatConverter.convert(
                        it
                    )
                }
            },
            status = takeIf { zaakrechten.lezen }?.let {
                zaak.status?.let {
                    ztcClientService.readStatustype(zrcClientService.readStatus(it).statustype).omschrijving
                }
            },
            behandelaar = takeIf { zaakrechten.lezen }?.let {
                zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)
                    .map { userConverter.convertUserId(it.betrokkeneIdentificatie.identificatie) }
                    .orElse(null)
            },
            groep = takeIf { zaakrechten.lezen }?.let {
                zgwApiService.findGroepForZaak(zaak)
                    .map { groupConverter.convertGroupId(it.betrokkeneIdentificatie.identificatie) }
                    .orElse(null)
            }
        )
    }
}
