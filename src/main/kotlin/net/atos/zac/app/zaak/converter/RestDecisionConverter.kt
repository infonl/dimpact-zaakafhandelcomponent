/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.converter

import jakarta.inject.Inject
import net.atos.client.zgw.brc.BrcClientService
import net.atos.client.zgw.brc.model.generated.Besluit
import net.atos.client.zgw.brc.model.generated.VervalredenEnum
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import net.atos.client.zgw.util.extractUuid
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.app.informatieobjecten.converter.RestInformatieobjectConverter
import net.atos.zac.app.zaak.model.RestDecision
import net.atos.zac.app.zaak.model.RestDecisionCreateData
import net.atos.zac.app.zaak.model.toRestBesluitType
import net.atos.zac.configuratie.ConfiguratieService
import nl.lifely.zac.util.NoArgConstructor
import java.time.LocalDate

@NoArgConstructor
class RestDecisionConverter @Inject constructor(
    private val brcClientService: BrcClientService,
    private val drcClientService: DrcClientService,
    private val restInformatieobjectConverter: RestInformatieobjectConverter,
    private val ztcClientService: ZtcClientService
) {
    fun convertToRestDecision(besluit: Besluit) =
        ztcClientService.readBesluittype(besluit.besluittype).let { besluitType ->
            RestDecision(
                uuid = besluit.url.extractUuid(),
                besluittype = besluitType.toRestBesluitType(),
                datum = besluit.datum,
                identificatie = besluit.identificatie,
                url = besluit.url,
                toelichting = besluit.toelichting,
                ingangsdatum = besluit.ingangsdatum,
                vervaldatum = besluit.vervaldatum,
                vervalreden = besluit.vervalreden,
                publicationDate = besluit.publicatiedatum,
                lastResponseDate = besluit.uiterlijkeReactiedatum,
                isIngetrokken = besluit.vervaldatum != null && (
                    besluit.vervalreden == VervalredenEnum.INGETROKKEN_BELANGHEBBENDE ||
                        besluit.vervalreden == VervalredenEnum.INGETROKKEN_OVERHEID
                    ),
                informatieobjecten = restInformatieobjectConverter.convertInformatieobjectenToREST(
                    listDecisionInformationObjects(besluit)
                )
            )
        }

    fun convertToBesluit(zaak: Zaak, besluitToevoegenGegevens: RestDecisionCreateData) =
        ztcClientService.readBesluittype(besluitToevoegenGegevens.besluittypeUuid).let { besluitType ->
            Besluit().apply {
                this.zaak = zaak.url
                besluittype = besluitType.url
                datum = LocalDate.now()
                ingangsdatum = besluitToevoegenGegevens.ingangsdatum
                vervaldatum = besluitToevoegenGegevens.vervaldatum
                besluitToevoegenGegevens.vervaldatum?.apply {
                    vervalreden = VervalredenEnum.TIJDELIJK
                }
                publicatiedatum = besluitToevoegenGegevens.publicationDate
                uiterlijkeReactiedatum = besluitToevoegenGegevens.lastResponseDate
                verantwoordelijkeOrganisatie = ConfiguratieService.VERANTWOORDELIJKE_ORGANISATIE
                toelichting = besluitToevoegenGegevens.toelichting
            }
        }

    private fun listDecisionInformationObjects(besluit: Besluit): List<EnkelvoudigInformatieObject> =
        brcClientService.listBesluitInformatieobjecten(besluit.url)
            .map { drcClientService.readEnkelvoudigInformatieobject(it.informatieobject) }
}
