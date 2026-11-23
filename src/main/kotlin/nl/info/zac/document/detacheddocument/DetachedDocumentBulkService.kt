/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.detacheddocument

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.WebApplicationException
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.shared.exception.ZgwErrorException
import nl.info.client.zgw.shared.exception.ZgwRuntimeException
import nl.info.client.zgw.shared.exception.ZgwValidationErrorException
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.document.detacheddocument.log.DetachedDocumentNotProcessed
import nl.info.zac.document.detacheddocument.model.DetachedDocumentBulkFailure
import nl.info.zac.document.detacheddocument.model.DetachedDocumentBulkFailureReason
import nl.info.zac.document.detacheddocument.model.DetachedDocumentBulkResult
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocument
import nl.info.zac.log.InformatieobjectEvent
import nl.info.zac.log.ZacLogger
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.jetty.http.HttpStatus
import java.util.UUID
import java.util.logging.Level

@ApplicationScoped
@NoArgConstructor
@AllOpen
class DetachedDocumentBulkService @Inject constructor(
    private val detachedDocumentService: DetachedDocumentService,
    private val drcClientService: DrcClientService,
    private val zrcClientService: ZrcClientService,
    private val policyService: PolicyService,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {
    companion object {
        const val DETACHED_DOCUMENTS_SOURCE = "ontkoppelde-documenten"

        private val LOG = ZacLogger.getLogger(DetachedDocumentBulkService::class)
    }

    fun delete(id: Long) {
        detachedDocumentService.find(id)?.let { removeDetachedDocument(it, rejectLocked = false) }
    }

    fun deleteInBulk(ids: List<Long>) = processInBulk(ids) { removeDetachedDocument(it, rejectLocked = true) }

    fun linkInBulk(ids: List<Long>, nieuweZaakID: String): DetachedDocumentBulkResult {
        val targetZaak = zrcClientService.readZaakByID(nieuweZaakID)
        val canWijzigenTargetZaak = policyService.readZaakRechten(targetZaak, loggedInUserInstance.get()).canWijzigen
        return processInBulk(ids) { linkDetachedDocument(it, targetZaak, canWijzigenTargetZaak) }
    }

    private fun processInBulk(
        ids: List<Long>,
        process: (DetachedDocument) -> Unit
    ): DetachedDocumentBulkResult {
        val processedIds = mutableListOf<Long>()
        val failures = mutableListOf<DetachedDocumentBulkFailure>()
        ids.distinct().forEach { id ->
            val detachedDocument = detachedDocumentService.find(id)
            val failureReason = if (detachedDocument == null) {
                DetachedDocumentBulkFailureReason.NOT_FOUND
            } else {
                processDocument(id, detachedDocument, process)
            }
            if (failureReason == null) {
                processedIds.add(id)
            } else {
                failures.add(DetachedDocumentBulkFailure(id, detachedDocument?.titel, failureReason))
            }
        }
        return DetachedDocumentBulkResult(processedIds = processedIds, failures = failures)
    }

    private fun WebApplicationException.toFailureReason() =
        when {
            isNotFound() -> DetachedDocumentBulkFailureReason.NOT_FOUND
            isClientError() -> DetachedDocumentBulkFailureReason.OPEN_ZAAK_ERROR
            else -> DetachedDocumentBulkFailureReason.OPEN_ZAAK_UNAVAILABLE
        }

    private fun processDocument(
        id: Long,
        detachedDocument: DetachedDocument,
        process: (DetachedDocument) -> Unit
    ): DetachedDocumentBulkFailureReason? =
        try {
            process(detachedDocument)
            null
        } catch (detachedDocumentLockedException: DetachedDocumentLockedException) {
            LOG.fine(
                DetachedDocumentNotProcessed(id, DetachedDocumentBulkFailureReason.LOCKED),
                detachedDocumentLockedException
            )
            DetachedDocumentBulkFailureReason.LOCKED
        } catch (detachedDocumentMissingInOpenZaakException: DetachedDocumentMissingInOpenZaakException) {
            LOG.fine(
                DetachedDocumentNotProcessed(id, DetachedDocumentBulkFailureReason.MISSING_IN_OPEN_ZAAK),
                detachedDocumentMissingInOpenZaakException
            )
            DetachedDocumentBulkFailureReason.MISSING_IN_OPEN_ZAAK
        } catch (illegalStateException: IllegalStateException) {
            LOG.fine(
                DetachedDocumentNotProcessed(id, DetachedDocumentBulkFailureReason.ALREADY_LINKED),
                illegalStateException
            )
            DetachedDocumentBulkFailureReason.ALREADY_LINKED
        } catch (_: PolicyException) {
            LOG.fine { DetachedDocumentNotProcessed(id, DetachedDocumentBulkFailureReason.NO_RIGHTS) }
            DetachedDocumentBulkFailureReason.NO_RIGHTS
        } catch (zgwErrorException: ZgwErrorException) {
            LOG.fine(
                DetachedDocumentNotProcessed(id, DetachedDocumentBulkFailureReason.OPEN_ZAAK_ERROR),
                zgwErrorException
            )
            DetachedDocumentBulkFailureReason.OPEN_ZAAK_ERROR
        } catch (zgwValidationErrorException: ZgwValidationErrorException) {
            LOG.fine(
                DetachedDocumentNotProcessed(id, DetachedDocumentBulkFailureReason.OPEN_ZAAK_ERROR),
                zgwValidationErrorException
            )
            DetachedDocumentBulkFailureReason.OPEN_ZAAK_ERROR
        } catch (zgwRuntimeException: ZgwRuntimeException) {
            LOG.severe(
                DetachedDocumentNotProcessed(id, DetachedDocumentBulkFailureReason.OPEN_ZAAK_UNAVAILABLE),
                zgwRuntimeException
            )
            DetachedDocumentBulkFailureReason.OPEN_ZAAK_UNAVAILABLE
        } catch (webApplicationException: WebApplicationException) {
            webApplicationException.toFailureReason().also {
                val level = if (webApplicationException.isClientError()) Level.FINE else Level.SEVERE
                LOG.log(level, DetachedDocumentNotProcessed(id, it), webApplicationException)
            }
        } catch (processingException: ProcessingException) {
            LOG.severe(
                DetachedDocumentNotProcessed(id, DetachedDocumentBulkFailureReason.OPEN_ZAAK_UNAVAILABLE),
                processingException
            )
            DetachedDocumentBulkFailureReason.OPEN_ZAAK_UNAVAILABLE
        }


    private fun removeDetachedDocument(detachedDocument: DetachedDocument, rejectLocked: Boolean) {
        val documentUUID = detachedDocument.documentUUID
        findEnkelvoudigInformatieobject(documentUUID)?.let { informatieobject ->
            if (rejectLocked && informatieobject.getLocked()) {
                throw DetachedDocumentLockedException("Informatieobject '$documentUUID' is vergrendeld")
            }
            val zaakInformatieobjecten = zrcClientService.listZaakinformatieobjecten(informatieobject)
            if (zaakInformatieobjecten.isNotEmpty()) {
                val zaakUuid = zaakInformatieobjecten.first().zaak.extractUuid()
                error("Informatieobject is gekoppeld aan zaak '$zaakUuid'")
            }
            drcClientService.deleteEnkelvoudigInformatieobject(documentUUID)
        }
        detachedDocument.id?.let(detachedDocumentService::deleteIfExists)
    }

    fun findEnkelvoudigInformatieobject(documentUUID: UUID): EnkelvoudigInformatieObject? =
        try {
            drcClientService.readEnkelvoudigInformatieobject(documentUUID)
        } catch (webApplicationException: WebApplicationException) {
            if (!webApplicationException.isNotFound()) {
                throw webApplicationException
            }
            LOG.info(
                InformatieobjectEvent(
                    type = InformatieobjectEvent.Type.DOCUMENT_MISSING_IN_OPEN_ZAAK,
                    informatieobjectUuid = documentUUID
                )
            )
            null
        }

    private fun readExistingInformatieobject(documentUUID: UUID): EnkelvoudigInformatieObject =
        findEnkelvoudigInformatieobject(documentUUID)
            ?: throw DetachedDocumentMissingInOpenZaakException(
                "Informatieobject '$documentUUID' bestaat niet in Open Zaak"
            )

    private fun linkDetachedDocument(
        detachedDocument: DetachedDocument,
        targetZaak: Zaak,
        canWijzigenTargetZaak: Boolean
    ) {
        val informatieobject = readExistingInformatieobject(detachedDocument.documentUUID)
        assertPolicy(
            canWijzigenTargetZaak && policyService.readDocumentRechten(informatieobject, targetZaak).canVerplaatsen
        )
        try {
            zrcClientService.koppelInformatieobject(
                informatieobject,
                targetZaak,
                "Verplaatst: $DETACHED_DOCUMENTS_SOURCE -> ${targetZaak.identificatie}"
            )
        } catch (zgwValidationErrorException: ZgwValidationErrorException) {
            // Open Zaak also refuses the link of a document that another employee deleted after it was read above.
            if (isConfirmedDeleted(detachedDocument.documentUUID)) {
                throw NotFoundException("Informatieobject '${detachedDocument.documentUUID}' is verwijderd")
            }
            throw zgwValidationErrorException
        }
        detachedDocumentService.deleteIfExists(detachedDocument.documentUUID)
    }

    private fun isConfirmedDeleted(documentUUID: UUID): Boolean =
        try {
            findEnkelvoudigInformatieobject(documentUUID) == null
        } catch (_: WebApplicationException) {
            false
        } catch (_: ZgwRuntimeException) {
            false
        } catch (_: ProcessingException) {
            false
        }
}

private fun WebApplicationException.isNotFound() = response?.status == HttpStatus.NOT_FOUND_404

private fun WebApplicationException.isClientError() =
    response?.status in HttpStatus.BAD_REQUEST_400 until HttpStatus.INTERNAL_SERVER_ERROR_500
