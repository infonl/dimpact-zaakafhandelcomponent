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
import nl.info.zac.document.detacheddocument.model.DetachedDocumentBulkFailure
import nl.info.zac.document.detacheddocument.model.DetachedDocumentBulkFailureReason
import nl.info.zac.document.detacheddocument.model.DetachedDocumentBulkResult
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocument
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.jetty.http.HttpStatus
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

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

        private val LOG = Logger.getLogger(DetachedDocumentBulkService::class.java.name)
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

    private fun processDocument(
        id: Long,
        detachedDocument: DetachedDocument,
        process: (DetachedDocument) -> Unit
    ): DetachedDocumentBulkFailureReason? =
        try {
            process(detachedDocument)
            null
        } catch (detachedDocumentLockedException: DetachedDocumentLockedException) {
            LOG.fine { failureMessage(id, detachedDocumentLockedException) }
            DetachedDocumentBulkFailureReason.LOCKED
        } catch (illegalStateException: IllegalStateException) {
            LOG.fine { failureMessage(id, illegalStateException) }
            DetachedDocumentBulkFailureReason.ALREADY_LINKED
        } catch (_: PolicyException) {
            LOG.fine { "Geen recht op ontkoppeld document '$id'" }
            DetachedDocumentBulkFailureReason.NO_RIGHTS
        } catch (zgwErrorException: ZgwErrorException) {
            LOG.fine { failureMessage(id, zgwErrorException) }
            DetachedDocumentBulkFailureReason.OPEN_ZAAK_ERROR
        } catch (zgwValidationErrorException: ZgwValidationErrorException) {
            LOG.fine { failureMessage(id, zgwValidationErrorException) }
            DetachedDocumentBulkFailureReason.OPEN_ZAAK_ERROR
        } catch (zgwRuntimeException: ZgwRuntimeException) {
            LOG.severe { failureMessage(id, zgwRuntimeException) }
            DetachedDocumentBulkFailureReason.OPEN_ZAAK_UNAVAILABLE
        } catch (webApplicationException: WebApplicationException) {
            val level = if (webApplicationException.isClientError()) Level.FINE else Level.SEVERE
            LOG.log(level) { failureMessage(id, webApplicationException) }
            when {
                webApplicationException.isNotFound() -> DetachedDocumentBulkFailureReason.NOT_FOUND
                webApplicationException.isClientError() -> DetachedDocumentBulkFailureReason.OPEN_ZAAK_ERROR
                else -> DetachedDocumentBulkFailureReason.OPEN_ZAAK_UNAVAILABLE
            }
        } catch (processingException: ProcessingException) {
            LOG.severe { failureMessage(id, processingException) }
            DetachedDocumentBulkFailureReason.OPEN_ZAAK_UNAVAILABLE
        }

    private fun failureMessage(id: Long, exception: Exception) =
        "Ontkoppeld document '$id' kon niet worden verwerkt: ${exception.message}"

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
            LOG.info { "Document met UUID '$documentUUID' wel gevonden in de database, maar niet in OpenZaak" }
            null
        }

    private fun WebApplicationException.isNotFound() = response?.status == HttpStatus.NOT_FOUND_404

    private fun WebApplicationException.isClientError() =
        response?.status in HttpStatus.BAD_REQUEST_400 until HttpStatus.INTERNAL_SERVER_ERROR_500

    private fun linkDetachedDocument(
        detachedDocument: DetachedDocument,
        targetZaak: Zaak,
        canWijzigenTargetZaak: Boolean
    ) {
        val informatieobject = drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
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
            findEnkelvoudigInformatieobject(detachedDocument.documentUUID)
                ?: throw NotFoundException("Informatieobject '${detachedDocument.documentUUID}' is verwijderd")
            throw zgwValidationErrorException
        }
        detachedDocumentService.deleteIfExists(detachedDocument.documentUUID)
    }
}
