/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.converter

import jakarta.inject.Inject
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.app.identity.converter.RestGroupConverter
import net.atos.zac.app.identity.converter.RestUserConverter
import net.atos.zac.app.identity.model.RestGroup
import net.atos.zac.app.identity.model.RestUser
import net.atos.zac.app.policy.converter.RestRechtenConverter
import net.atos.zac.policy.PolicyService
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.app.zaak.model.RestZaakOverzicht

@Suppress("LongParameterList")
class RestZaakOverzichtConverter @Inject constructor(
    private val ztcClientService: ZtcClientService,
    private val zgwApiService: ZGWApiService,
    private val zaakResultaatConverter: RestZaakResultaatConverter,
    private val groupConverter: RestGroupConverter,
    private val userConverter: RestUserConverter,
    private val openstaandeTakenConverter: RestOpenstaandeTakenConverter,
    private val policyService: PolicyService,
    private val zrcClientService: ZrcClientService,
) {
    fun convert(zaak: Zaak): RestZaakOverzicht {
        val zaaktype = ztcClientService.readZaaktype(zaak.zaaktype)
        val zaakrechten = policyService.readZaakRechten(zaak, zaaktype)
        return RestZaakOverzicht(
            uuid = zaak.uuid,
            identificatie = zaak.identificatie,
            rechten = RestRechtenConverter.convert(zaakrechten),
            startdatum = zaakrechten.lezen.takeIf { it }?.let { zaak.startdatum },
            einddatum = zaakrechten.lezen.takeIf { it }?.let { zaak.einddatum },
            einddatumGepland = zaakrechten.lezen.takeIf { it }?.let { zaak.einddatumGepland },
            uiterlijkeEinddatumAfdoening = zaakrechten.lezen.takeIf { it }?.let { zaak.uiterlijkeEinddatumAfdoening },
            toelichting = zaakrechten.lezen.takeIf { it }?.let { zaak.toelichting },
            omschrijving = zaakrechten.lezen.takeIf { it }?.let { zaak.omschrijving },
            zaaktype = zaakrechten.lezen.takeIf { it }?.let { zaaktype.omschrijving },
            openstaandeTaken = openstaandeTakenConverter.convert(zaak.uuid),
            resultaat = zaakrechten.lezen.takeIf { it }?.let { zaak.resultaat?.let(zaakResultaatConverter::convert) },
            status = zaakrechten.lezen.takeIf { it }?.let { zaak.status }
                ?.let { zrcClientService.readStatus(it).statustype }
                ?.let { ztcClientService.readStatustype(it).omschrijving },
            behandelaar = zaakrechten.lezen.takeIf { it }?.let { getBehandelaarForZaak(zaak) },
            groep = zaakrechten.lezen.takeIf { it }?.let { getGroupForZaak(zaak) }
        )
    }

    fun convertForDisplay(zaak: Zaak): RestZaakOverzicht {
        val zaakType = ztcClientService.readZaaktype(zaak.zaaktype)
        val zaakrechten = policyService.readZaakRechten(zaak, zaakType)
        return RestZaakOverzicht(
            identificatie = zaak.identificatie,
        ).apply {
            if (zaakrechten.lezen) {
                startdatum = zaak.startdatum
                omschrijving = zaak.omschrijving
                zaaktype = zaakType.omschrijving
            }
        }
    }

    private fun getBehandelaarForZaak(
        zaak: Zaak
    ): RestUser? = zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)
        ?.betrokkeneIdentificatie
        ?.let { userConverter.convertUserId(it.identificatie) }

    private fun getGroupForZaak(
        zaak: Zaak
    ): RestGroup? = zgwApiService.findGroepForZaak(zaak)
        ?.betrokkeneIdentificatie
        ?.let { groupConverter.convertGroupId(it.identificatie) }
}
