/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.besluit

import jakarta.inject.Inject
import net.atos.zac.util.time.PeriodUtil
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.brc.model.generated.Besluit
import nl.info.client.zgw.brc.model.generated.BesluitInformatieObject
import nl.info.client.zgw.brc.model.generated.VervalredenEnum
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.app.zaak.converter.RestDecisionConverter
import nl.info.zac.app.zaak.model.RestDecisionChangeData
import nl.info.zac.app.zaak.model.RestDecisionCreateData
import nl.info.zac.app.zaak.model.RestDecisionWithdrawalData
import nl.info.zac.app.zaak.model.updateDecisionWithDecisionChangeData
import org.apache.commons.collections4.CollectionUtils
import java.time.LocalDate
import java.time.Period
import java.util.UUID
import java.util.logging.Logger

class BesluitService @Inject constructor(
    private val brcClientService: BrcClientService,
    private val drcClientService: DrcClientService,
    private val ztcClientService: ZtcClientService,
    private val restDecisionConverter: RestDecisionConverter,
) {
    companion object {
        private val LOG = Logger.getLogger(BesluitService::class.java.name)

        private const val CREATE_BESLUIT_EXPLANATION = "Aanmaken besluit"
        private const val CHANGE_BESLUIT_EXPLANATION = "Wijzigen besluit"
    }

    fun readBesluit(restDecisionWithdrawalData: RestDecisionWithdrawalData): Besluit =
        brcClientService.readBesluit(restDecisionWithdrawalData.besluitUuid).apply {
            vervaldatum = restDecisionWithdrawalData.vervaldatum
            vervalreden = VervalredenEnum.fromValue(restDecisionWithdrawalData.vervalreden.lowercase())
        }

    fun createBesluit(zaak: Zaak, besluitToevoegenGegevens: RestDecisionCreateData): Besluit {
        validateBesluitPublicationDates(
            besluitToevoegenGegevens.besluittypeUuid,
            besluitToevoegenGegevens.publicationDate,
            besluitToevoegenGegevens.lastResponseDate
        )

        val besluitToCreate = restDecisionConverter.convertToBesluit(zaak, besluitToevoegenGegevens)

        return brcClientService.createBesluit(besluitToCreate).also {
            createBesluitInformationObjects(besluitToevoegenGegevens, it)
        }
    }

    @Suppress("NestedBlockDepth")
    private fun validateBesluitPublicationDates(
        besluitTypeUUID: UUID,
        publicationDate: LocalDate?,
        responseDate: LocalDate?
    ) =
        ztcClientService.readBesluittype(besluitTypeUUID).run {
            if (!publicatieIndicatie) {
                if (publicationDate != null || responseDate != null) {
                    throw BesluitPublicationDisabledException(
                        "Besluit type with UUID '${url.extractUuid()}' and name " +
                            "'$omschrijving' cannot have publication or response dates"
                    )
                }
            }
            if (publicationDate == null && responseDate != null) {
                throw BesluitPublicationDateMissingException()
            }
            if (publicationDate != null && responseDate == null) {
                throw BesluitResponseDateMissingException()
            }
            responseDate?.let {
                PeriodUtil.numberOfDaysFromToday(Period.parse(reactietermijn)).toLong().let { responseDays ->
                    publicationDate?.plusDays(responseDays).let { calculatedLatestResponseDate ->
                        if (it.isBefore(calculatedLatestResponseDate)) {
                            throw BesluitResponseDateInvalidException(
                                "Response date $responseDate is before " +
                                    "calculated response date $calculatedLatestResponseDate"
                            )
                        }
                    }
                }
            }
        }

    private fun createBesluitInformationObjects(
        besluitToevoegenGegevens: RestDecisionCreateData,
        createdBesluit: Besluit
    ) {
        besluitToevoegenGegevens.informatieobjecten?.forEach { informatieobjectUuid ->
            drcClientService.readEnkelvoudigInformatieobject(informatieobjectUuid).let { informatieobject ->
                BesluitInformatieObject().apply {
                    this.informatieobject = informatieobject.url
                    this.besluit = createdBesluit.url
                }.let {
                    brcClientService.createBesluitInformatieobject(it, CREATE_BESLUIT_EXPLANATION)
                }
            }
        }
    }

    fun updateBesluit(
        besluit: Besluit,
        restDecisionChangeData: RestDecisionChangeData
    ) {
        validateBesluitPublicationDates(
            besluit.besluittype.extractUuid(),
            restDecisionChangeData.publicationDate,
            restDecisionChangeData.lastResponseDate
        )

        besluit.updateDecisionWithDecisionChangeData(restDecisionChangeData).also {
            brcClientService.updateBesluit(it, restDecisionChangeData.reden)
        }
        restDecisionChangeData.informatieobjecten?.let {
            updateBesluitInformationObjects(besluit, it)
        }
    }

    private fun updateBesluitInformationObjects(
        besluit: Besluit,
        newDocumentUuids: List<UUID>
    ) {
        val besluitInformatieobjecten = brcClientService.listBesluitInformatieobjecten(besluit.url)
        val currentDocumentUuids = besluitInformatieobjecten
            .map { it.informatieobject.extractUuid() }
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
                brcClientService.createBesluitInformatieobject(it, CHANGE_BESLUIT_EXPLANATION)
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
