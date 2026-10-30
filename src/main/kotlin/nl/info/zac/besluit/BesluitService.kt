/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.besluit

import jakarta.inject.Inject
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.brc.model.NillableDatesBesluitPatch
import nl.info.client.zgw.brc.model.generated.Besluit
import nl.info.client.zgw.brc.model.generated.BesluitInformatieObject
import nl.info.client.zgw.brc.model.generated.VervalredenEnum
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.util.time.numberOfDaysFromToday
import org.apache.commons.collections4.CollectionUtils
import java.time.LocalDate
import java.time.Period
import java.util.Locale
import java.util.UUID
import java.util.logging.Logger

class BesluitService @Inject constructor(
    private val brcClientService: BrcClientService,
    private val drcClientService: DrcClientService,
    private val ztcClientService: ZtcClientService,
) {
    companion object {
        private val LOG = Logger.getLogger(BesluitService::class.java.name)

        private const val CREATE_BESLUIT_EXPLANATION = "Aanmaken besluit"
        private const val CHANGE_BESLUIT_EXPLANATION = "Wijzigen besluit"
    }

    fun readBesluit(besluitUuid: UUID, vervaldatum: LocalDate?, vervalreden: VervalredenEnum): Besluit =
        brcClientService.readBesluit(besluitUuid).apply {
            this.vervaldatum = vervaldatum
            this.vervalreden = vervalreden
        }

    fun createBesluit(besluit: Besluit, informatieobjectUuids: List<UUID>?): Besluit {
        validateBesluitPublicationDates(
            besluit.besluittype.extractUuid(),
            besluit.publicatiedatum,
            besluit.uiterlijkeReactiedatum
        )

        return brcClientService.createBesluit(besluit).also {
            createBesluitInformationObjects(informatieobjectUuids, it)
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
                Period.parse(reactietermijn).numberOfDaysFromToday().toLong().let { responseDays ->
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
        informatieobjectUuids: List<UUID>?,
        createdBesluit: Besluit
    ) {
        informatieobjectUuids?.forEach { informatieobjectUuid ->
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
        besluitPatch: NillableDatesBesluitPatch,
        informatieobjectUuids: List<UUID>?,
        reason: String
    ): Besluit {
        validateBesluitPublicationDates(
            besluit.besluittype.extractUuid(),
            besluitPatch.publicatiedatum,
            besluitPatch.uiterlijkeReactiedatum
        )

        return brcClientService.patchBesluit(
            besluitUuid = besluit.url.extractUuid(),
            besluit = besluitPatch,
            auditExplanation = reason
        ).also {
            if (informatieobjectUuids != null) updateBesluitInformationObjects(besluit, informatieobjectUuids)
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
            getBesluitWithdrawalExplanation(besluit.vervalreden)?.let { String.format(Locale.ROOT, it, reden) }
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
