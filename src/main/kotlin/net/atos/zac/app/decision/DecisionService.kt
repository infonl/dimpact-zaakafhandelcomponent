/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.decision

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
import net.atos.zac.app.zaak.converter.RestDecisionConverter
import net.atos.zac.app.zaak.model.RestDecisionChangeData
import net.atos.zac.app.zaak.model.RestDecisionCreateData
import net.atos.zac.app.zaak.model.RestDecisionWithdrawalData
import net.atos.zac.app.zaak.model.updateDecisionWithDecisionChangeData
import net.atos.zac.util.time.PeriodUtil
import org.apache.commons.collections4.CollectionUtils
import java.time.LocalDate
import java.time.Period
import java.util.UUID
import java.util.logging.Logger

class DecisionService @Inject constructor(
    private val brcClientService: BrcClientService,
    private val drcClientService: DrcClientService,
    private val ztcClientService: ZtcClientService,
    private val zrcClientService: ZrcClientService,
    private val zgwApiService: ZGWApiService,
    private val restDecisionConverter: RestDecisionConverter,
) {
    companion object {
        private val LOG = Logger.getLogger(DecisionService::class.java.name)

        private const val CREATE_DECISION_EXPLANATION = "Aanmaken besluit"
        private const val CHANGE_DECISION_EXPLANATION = "Wijzigen besluit"
    }

    fun readDecision(restDecisionWithdrawalData: RestDecisionWithdrawalData): Besluit =
        brcClientService.readBesluit(restDecisionWithdrawalData.besluitUuid).apply {
            vervaldatum = restDecisionWithdrawalData.vervaldatum
            vervalreden = VervalredenEnum.fromValue(restDecisionWithdrawalData.vervalreden.lowercase())
        }

    fun createDecision(zaak: Zaak, besluitToevoegenGegevens: RestDecisionCreateData): Besluit {
        validateDecisionPublicationDates(
            besluitToevoegenGegevens.besluittypeUuid,
            besluitToevoegenGegevens.publicationDate,
            besluitToevoegenGegevens.lastResponseDate
        )

        val besluitToCreate = restDecisionConverter.convertToBesluit(zaak, besluitToevoegenGegevens)
        zaak.resultaat?.let {
            zgwApiService.updateResultaatForZaak(zaak, besluitToevoegenGegevens.resultaattypeUuid, null)
        } ?: run {
            zgwApiService.createResultaatForZaak(zaak, besluitToevoegenGegevens.resultaattypeUuid, null)
        }

        return brcClientService.createBesluit(besluitToCreate).also {
            createDecisionInformationObjects(besluitToevoegenGegevens, it)
        }
    }

    @Suppress("NestedBlockDepth")
    private fun validateDecisionPublicationDates(
        besluitTypeUUID: UUID,
        publicationDate: LocalDate?,
        responseDate: LocalDate?
    ) =
        ztcClientService.readBesluittype(besluitTypeUUID).run {
            if (!publicatieIndicatie) {
                if (publicationDate != null || responseDate != null) {
                    throw DecisionPublicationDisabledException(
                        "Besluit type with UUID '${url.extractUuid()}' and name " +
                            "'$omschrijving' cannot have publication or response dates"
                    )
                }
            }
            if (publicationDate == null && responseDate != null) {
                throw DecisionPublicationDateMissingException()
            }
            if (publicationDate != null && responseDate == null) {
                throw DecisionResponseDateMissingException()
            }
            responseDate?.let {
                PeriodUtil.numberOfDaysFromToday(Period.parse(reactietermijn)).toLong().let { responseDays ->
                    publicationDate?.plusDays(responseDays).let { calculatedLatestResponseDate ->
                        if (it.isBefore(calculatedLatestResponseDate)) {
                            throw DecisionResponseDateInvalidException(
                                "Response date $responseDate is before " +
                                    "calculated response date $calculatedLatestResponseDate"
                            )
                        }
                    }
                }
            }
        }

    private fun createDecisionInformationObjects(
        besluitToevoegenGegevens: RestDecisionCreateData,
        createdBesluit: Besluit
    ) {
        besluitToevoegenGegevens.informatieobjecten?.forEach { informatieobjectUuid ->
            drcClientService.readEnkelvoudigInformatieobject(informatieobjectUuid).let { informatieobject ->
                BesluitInformatieObject().apply {
                    this.informatieobject = informatieobject.url
                    this.besluit = createdBesluit.url
                }.let {
                    brcClientService.createBesluitInformatieobject(it, CREATE_DECISION_EXPLANATION)
                }
            }
        }
    }

    fun updateDecision(
        zaak: Zaak,
        besluit: Besluit,
        restDecisionChangeData: RestDecisionChangeData
    ) {
        validateDecisionPublicationDates(
            besluit.besluittype.extractUuid(),
            restDecisionChangeData.publicationDate,
            restDecisionChangeData.lastResponseDate
        )

        besluit.updateDecisionWithDecisionChangeData(restDecisionChangeData).also {
            brcClientService.updateBesluit(it, restDecisionChangeData.reden)
        }
        zaak.resultaat?.let {
            zrcClientService.readResultaat(it).let { zaakresultaat ->
                val resultaattype = ztcClientService.readResultaattype(restDecisionChangeData.resultaattypeUuid)
                if (!extractedUuidIsEqual(zaakresultaat.resultaattype, resultaattype.url)) {
                    zrcClientService.deleteResultaat(zaakresultaat.uuid)
                    zgwApiService.createResultaatForZaak(zaak, restDecisionChangeData.resultaattypeUuid, null)
                }
            }
        }
        restDecisionChangeData.informatieobjecten?.let {
            updateDecisionInformationObjects(besluit, it)
        }
    }

    private fun updateDecisionInformationObjects(
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
        documentUuidsToAdd.forEach { documentUUID ->
            drcClientService.readEnkelvoudigInformatieobject(documentUUID).let { enkelvoudigInformatieObject ->
                BesluitInformatieObject().apply {
                    this.informatieobject = enkelvoudigInformatieObject.url
                    this.besluit = besluit.url
                }
            }.let {
                brcClientService.createBesluitInformatieobject(it, CHANGE_DECISION_EXPLANATION)
            }
        }
    }

    fun withdrawDecision(besluit: Besluit, reden: String): Besluit =
        brcClientService.updateBesluit(
            besluit,
            getDecisionWithdrawalExplanation(besluit.vervalreden)?.let { String.format(it, reden) }
        )

    private fun getDecisionWithdrawalExplanation(withdrawalReason: VervalredenEnum): String? {
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
