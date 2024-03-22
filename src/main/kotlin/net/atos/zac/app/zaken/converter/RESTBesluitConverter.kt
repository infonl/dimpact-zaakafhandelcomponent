/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.converter

import jakarta.inject.Inject
import net.atos.client.zgw.brc.BRCClientService
import net.atos.client.zgw.brc.model.generated.Besluit
import net.atos.client.zgw.brc.model.generated.Besluit.VervalredenEnum
import net.atos.client.zgw.drc.DRCClientService
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.zac.app.informatieobjecten.converter.RESTInformatieobjectConverter
import net.atos.zac.app.zaken.model.RESTBesluit
import net.atos.zac.app.zaken.model.RESTBesluitIntrekkenGegevens
import net.atos.zac.app.zaken.model.RESTBesluitVastleggenGegevens
import net.atos.zac.app.zaken.model.RESTBesluitWijzigenGegevens
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.util.UriUtil
import java.time.LocalDate
import java.util.stream.Collectors

class RESTBesluitConverter {
    @Inject
    private lateinit var restBesluittypeConverter: RESTBesluittypeConverter

    @Inject
    private lateinit var ztcClientService: ZTCClientService

    @Inject
    private lateinit var brcClientService: BRCClientService

    @Inject
    private lateinit var informatieobjectConverter: RESTInformatieobjectConverter

    @Inject
    private lateinit var drcClientService: DRCClientService

    fun convertToRESTBesluit(besluit: Besluit) = RESTBesluit(
        uuid = UriUtil.uuidFromURI(besluit.url),
        besluittype = restBesluittypeConverter.convertToRESTBesluittype(besluit.besluittype),
        datum = besluit.datum,
        identificatie = besluit.identificatie,
        url = besluit.url,
        toelichting = besluit.toelichting,
        ingangsdatum = besluit.ingangsdatum,
        vervaldatum = besluit.vervaldatum,
        vervalreden = besluit.vervalreden,
        isIngetrokken = besluit.vervaldatum != null && (
            besluit.vervalreden == VervalredenEnum.INGETROKKEN_BELANGHEBBENDE ||
                besluit.vervalreden == VervalredenEnum.INGETROKKEN_OVERHEID
            ),
        informatieobjecten = informatieobjectConverter.convertInformatieobjectenToREST(
            listBesluitInformatieobjecten(besluit)
        )
    )

    fun convertToRESTBesluit(besluiten: List<Besluit>) = besluiten.stream()
        .map { besluit -> convertToRESTBesluit(besluit) }
        .toList()

    fun convertToBesluit(zaak: Zaak, besluitToevoegenGegevens: RESTBesluitVastleggenGegevens): Besluit {
        val besluit = Besluit()
        besluit.zaak = zaak.url
        besluit.besluittype = ztcClientService.readBesluittype(besluitToevoegenGegevens.besluittypeUuid!!).url
        besluit.datum = LocalDate.now()
        besluit.ingangsdatum = besluitToevoegenGegevens.ingangsdatum
        besluit.vervaldatum = besluitToevoegenGegevens.vervaldatum
        if (besluitToevoegenGegevens.vervaldatum != null) {
            besluit.vervalreden = VervalredenEnum.TIJDELIJK
        }
        besluit.verantwoordelijkeOrganisatie = ConfiguratieService.VERANTWOORDELIJKE_ORGANISATIE
        besluit.toelichting = besluitToevoegenGegevens.toelichting
        return besluit
    }

    fun convertToBesluit(besluit: Besluit, besluitWijzigenGegevens: RESTBesluitWijzigenGegevens): Besluit {
        besluit.toelichting = besluitWijzigenGegevens.toelichting
        besluit.ingangsdatum = besluitWijzigenGegevens.ingangsdatum
        besluit.vervaldatum = besluitWijzigenGegevens.vervaldatum
        if (besluit.vervaldatum != null) {
            besluit.vervalreden = VervalredenEnum.TIJDELIJK
        }
        return besluit
    }

    fun convertToBesluit(
        besluit: Besluit,
        besluitIntrekkenGegevens: RESTBesluitIntrekkenGegevens
    ): Besluit {
        besluit.vervaldatum = besluitIntrekkenGegevens.vervaldatum
        besluit.vervalreden = VervalredenEnum.valueOf(besluitIntrekkenGegevens.vervalreden!!)
        return besluit
    }

    fun listBesluitInformatieobjecten(besluit: Besluit): List<EnkelvoudigInformatieObject> {
        return brcClientService.listBesluitInformatieobjecten(besluit.url).stream()
            .map { besluitInformatieobject ->
                drcClientService.readEnkelvoudigInformatieobject(besluitInformatieobject.informatieobject)
            }
            .collect(Collectors.toList())
    }
}
