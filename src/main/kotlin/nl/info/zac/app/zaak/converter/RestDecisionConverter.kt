/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.converter

import jakarta.inject.Inject
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.app.informatieobjecten.converter.RestInformatieobjectConverter
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.brc.model.generated.Besluit
import nl.info.client.zgw.brc.model.generated.VervalredenEnum
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.app.zaak.model.RestDecision
import nl.info.zac.app.zaak.model.RestDecisionCreateData
import nl.info.zac.app.zaak.model.toRestDecisionType
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate

@NoArgConstructor
class RestDecisionConverter @Inject constructor(
    private val brcClientService: BrcClientService,
    private val drcClientService: DrcClientService,
    private val restInformatieobjectConverter: RestInformatieobjectConverter,
    private val ztcClientService: ZtcClientService,
    private val configuratieService: ConfiguratieService
) {
    fun convertToRestDecision(besluit: Besluit) =
        ztcClientService.readBesluittype(besluit.besluittype).let { besluitType ->
            RestDecision(
                uuid = besluit.url.extractUuid(),
                besluittype = besluitType.toRestDecisionType(),
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
                verantwoordelijkeOrganisatie = configuratieService.readVerantwoordelijkeOrganisatie()
                toelichting = besluitToevoegenGegevens.toelichting
            }
        }

    private fun listDecisionInformationObjects(besluit: Besluit): List<EnkelvoudigInformatieObject> =
        brcClientService.listBesluitInformatieobjecten(besluit.url)
            .map { drcClientService.readEnkelvoudigInformatieobject(it.informatieobject) }
}
