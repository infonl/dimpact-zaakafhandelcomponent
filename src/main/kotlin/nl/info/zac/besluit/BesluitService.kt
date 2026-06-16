/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.besluit

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
import nl.info.zac.app.zaak.converter.RestBesluitConverter
import nl.info.zac.app.zaak.model.besluit.RestBesluitChangeData
import nl.info.zac.app.zaak.model.besluit.RestBesluitCreateData
import nl.info.zac.app.zaak.model.besluit.RestBesluitWithdrawalData
import nl.info.zac.app.zaak.model.besluit.updateBesluitWithBesluitChangeData
import org.apache.commons.collections4.CollectionUtils
import java.time.LocalDate
import java.time.Period
import java.util.UUID
import java.util.logging.Logger

class BesluitService @Inject constructor(
    private val brcClientService: BrcClientService,
    private val drcClientService: DrcClientService,
    private val ztcClientService: ZtcClientService,
    private val restBesluitConverter: RestBesluitConverter,
) {
    companion object {
        private val LOG = Logger.getLogger(BesluitService::class.java.name)

        private const val CREATE_BESLUIT_EXPLANATION = "Aanmaken besluit"
        private const val CHANGE_BESLUIT_EXPLANATION = "Wijzigen besluit"
    }

    fun readBesluit(restBesluitWithdrawalData: RestBesluitWithdrawalData): Besluit =
        brcClientService.readBesluit(restBesluitWithdrawalData.besluitUuid).apply {
            vervaldatum = restBesluitWithdrawalData.vervaldatum
            vervalreden = VervalredenEnum.fromValue(restBesluitWithdrawalData.vervalreden.lowercase())
        }

    fun createBesluit(zaak: Zaak, besluitToevoegenGegevens: RestBesluitCreateData): Besluit {
        validateBesluitPublicationDates(
            besluitToevoegenGegevens.besluittypeUuid,
            besluitToevoegenGegevens.publicationDate,
            besluitToevoegenGegevens.lastResponseDate
        )

        val besluitToCreate = restBesluitConverter.convertToBesluit(zaak, besluitToevoegenGegevens)

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
        besluitToevoegenGegevens: RestBesluitCreateData,
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
        restBesluitChangeData: RestBesluitChangeData
    ) {
        validateBesluitPublicationDates(
            besluit.besluittype.extractUuid(),
            restBesluitChangeData.publicationDate,
            restBesluitChangeData.lastResponseDate
        )

        besluit.updateBesluitWithBesluitChangeData(restBesluitChangeData).also {
            brcClientService.updateBesluit(it, restBesluitChangeData.reden)
        }
        restBesluitChangeData.informatieobjecten?.let {
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
