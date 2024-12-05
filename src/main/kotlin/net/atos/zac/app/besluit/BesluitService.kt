/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.besluit

import jakarta.inject.Inject
import net.atos.client.zgw.brc.BrcClientService
import net.atos.client.zgw.brc.model.generated.Besluit
import net.atos.client.zgw.brc.model.generated.BesluitInformatieObject
import net.atos.client.zgw.brc.model.generated.VervalredenEnum
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.util.extractUuid
import net.atos.client.zgw.util.extractedUuidIsEqual
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.app.zaak.converter.RestBesluitConverter
import net.atos.zac.app.zaak.model.RestBesluitIntrekkenGegevens
import net.atos.zac.app.zaak.model.RestBesluitVastleggenGegevens
import net.atos.zac.app.zaak.model.RestBesluitWijzigenGegevens
import net.atos.zac.app.zaak.model.updateBesluitWithBesluitWijzigenGegevens
import org.apache.commons.collections4.CollectionUtils
import java.time.LocalDate
import java.util.UUID
import java.util.logging.Logger

class BesluitService @Inject constructor(
    private val brcClientService: BrcClientService,
    private val drcClientService: DrcClientService,
    private val ztcClientService: ZtcClientService,
    private val zrcClientService: ZrcClientService,
    private val zgwApiService: ZGWApiService,
    private val restBesluitConverter: RestBesluitConverter,
) {
    companion object {
        private val LOG = Logger.getLogger(BrcClientService::class.java.name)

        private const val AANMAKEN_BESLUIT_TOELICHTING = "Aanmaken besluit"
        private const val WIJZIGEN_BESLUIT_TOELICHTING = "Wijzigen besluit"
    }

    fun readBesluit(restBesluitIntrekkenGegevens: RestBesluitIntrekkenGegevens): Besluit =
        brcClientService.readBesluit(restBesluitIntrekkenGegevens.besluitUuid).apply {
            vervaldatum = restBesluitIntrekkenGegevens.vervaldatum
            vervalreden = VervalredenEnum.fromValue(restBesluitIntrekkenGegevens.vervalreden.lowercase())
        }

    fun createBesluit(zaak: Zaak, besluitToevoegenGegevens: RestBesluitVastleggenGegevens): Besluit {
        validateBesluitData(
            besluitToevoegenGegevens.besluittypeUuid,
            besluitToevoegenGegevens.publicatiedatum,
            besluitToevoegenGegevens.uiterlijkeReactiedatum
        )

        val besluitToCreate = restBesluitConverter.convertToBesluit(zaak, besluitToevoegenGegevens)
        zaak.resultaat?.let {
            zgwApiService.updateResultaatForZaak(zaak, besluitToevoegenGegevens.resultaattypeUuid, null)
        } ?: run {
            zgwApiService.createResultaatForZaak(zaak, besluitToevoegenGegevens.resultaattypeUuid, null)
        }

        return brcClientService.createBesluit(besluitToCreate).also {
            createBesluitInformationObjects(besluitToevoegenGegevens, it)
        }
    }

    private fun validateBesluitData(
        besluitTypeUUID: UUID,
        publicationDate: LocalDate?,
        reactionDate: LocalDate?
    ) =
        ztcClientService.readBesluittype(besluitTypeUUID).run {
            if (publicatieIndicatie && (publicationDate != null || reactionDate != null)) {
                throw BesluitException(
                    "Besluit type with UUID '${url.extractUuid()}' and name " +
                        "'$omschrijving' cannot have publication or reaction dates"
                )
            }
        }

    private fun createBesluitInformationObjects(
        besluitToevoegenGegevens: RestBesluitVastleggenGegevens,
        createdBesluit: Besluit
    ) {
        besluitToevoegenGegevens.informatieobjecten?.forEach { informatieobjectUuid ->
            drcClientService.readEnkelvoudigInformatieobject(informatieobjectUuid).let { informatieobject ->
                BesluitInformatieObject().apply {
                    this.informatieobject = informatieobject.url
                    this.besluit = createdBesluit.url
                }.let {
                    brcClientService.createBesluitInformatieobject(
                        it,
                        AANMAKEN_BESLUIT_TOELICHTING
                    )
                }
            }
        }
    }

    fun updateBesluit(zaak: Zaak, restBesluitWijzigenGegevens: RestBesluitWijzigenGegevens) {
        val besluit = brcClientService.readBesluit(restBesluitWijzigenGegevens.besluitUuid)
        validateBesluitData(
            besluit.besluittype.extractUuid(),
            restBesluitWijzigenGegevens.publicatiedatum,
            restBesluitWijzigenGegevens.uiterlijkeReactiedatum
        )

        besluit.updateBesluitWithBesluitWijzigenGegevens(restBesluitWijzigenGegevens).also {
            brcClientService.updateBesluit(it, restBesluitWijzigenGegevens.reden)
        }
        zaak.resultaat?.let {
            zrcClientService.readResultaat(it).let { zaakresultaat ->
                val resultaattype = ztcClientService.readResultaattype(restBesluitWijzigenGegevens.resultaattypeUuid)
                if (!extractedUuidIsEqual(zaakresultaat.resultaattype, resultaattype.url)) {
                    zrcClientService.deleteResultaat(zaakresultaat.uuid)
                    zgwApiService.createResultaatForZaak(
                        zaak,
                        restBesluitWijzigenGegevens.resultaattypeUuid,
                        null
                    )
                }
            }
        }
        restBesluitWijzigenGegevens.informatieobjecten?.let {
            updateBesluitInformatieobjecten(besluit, it)
        }
    }

    private fun updateBesluitInformatieobjecten(
        besluit: Besluit,
        newDocumentUuids: List<UUID>
    ) {
        val besluitInformatieobjecten = brcClientService.listBesluitInformatieobjecten(besluit.url)
        val currentDocumentUuids = besluitInformatieobjecten
            .map { it.informatieobject.extractUuid() }
            .toList()
        val documentUuidsToRemove = CollectionUtils.subtract(currentDocumentUuids, newDocumentUuids)
        val documentUuidsToAdd = CollectionUtils.subtract(newDocumentUuids, currentDocumentUuids)
        documentUuidsToRemove.forEach { teVerwijderenInformatieobject ->
            besluitInformatieobjecten
                .filter { it.informatieobject.extractUuid() == teVerwijderenInformatieobject }
                .forEach { brcClientService.deleteBesluitinformatieobject(it.url.extractUuid()) }
        }
        documentUuidsToAdd.forEach { documentUri ->
            drcClientService.readEnkelvoudigInformatieobject(documentUri).let { enkelvoudigInformatieObject ->
                BesluitInformatieObject().apply {
                    this.informatieobject = enkelvoudigInformatieObject.url
                    this.besluit = besluit.url
                }
            }.let {
                brcClientService.createBesluitInformatieobject(
                    it,
                    WIJZIGEN_BESLUIT_TOELICHTING
                )
            }
        }
    }

    fun withdrawBesluit(besluit: Besluit, reden: String): Besluit =
        brcClientService.updateBesluit(
            besluit,
            getBesluitWithdrawalExplanation(besluit.vervalreden)?.let { String.format(it, reden) }
        )

    private fun getBesluitWithdrawalExplanation(withdrawalReason: VervalredenEnum): String? {
        return when (withdrawalReason) {
            VervalredenEnum.INGETROKKEN_OVERHEID -> "Overheid: %s"
            VervalredenEnum.INGETROKKEN_BELANGHEBBENDE -> "Belanghebbende: %s"
            else -> {
                LOG.info("Unknown besluit withdrawal reason: '$withdrawalReason'. Returning 'null'.")
                null
            }
        }
    }
}
