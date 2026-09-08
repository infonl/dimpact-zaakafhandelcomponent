/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.drc

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import nl.info.client.zgw.shared.model.Results
import nl.info.client.zgw.shared.exception.ZgwRuntimeException
import nl.info.client.zgw.shared.exception.ZgwValidationErrorException
import nl.info.client.zgw.shared.model.audit.AuditTrailRegel
import nl.info.client.zgw.drc.exception.DrcRuntimeException
import nl.info.client.zgw.drc.model.BestandsDeelInputStream
import nl.info.client.zgw.drc.model.BestandsDeelMultipartBody
import nl.info.client.zgw.drc.model.EnkelvoudigInformatieobjectListParameters
import nl.info.client.zgw.drc.model.generated.BestandsDeel
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectCreateLockRequest
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectCreateLockSub
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectWithLockRequest
import nl.info.client.zgw.drc.model.generated.Gebruiksrechten
import nl.info.client.zgw.drc.model.generated.LockEnkelvoudigInformatieObject
import nl.info.client.zgw.util.ZgwClientHeadersFactory
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.util.validateZgwApiUri
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.document.content.DocumentContent
import nl.info.zac.document.content.InMemoryDocumentContent
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.util.toBase64String
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.io.FilterInputStream
import java.io.InputStream
import java.net.URI
import java.util.UUID
import java.util.logging.Logger

@ApplicationScoped
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions")
class DrcClientService @Inject constructor(
    @RestClient private val drcClient: DrcClient,
    private val zgwClientHeadersFactory: ZgwClientHeadersFactory,
    private val configurationService: ConfigurationService
) {
    companion object {
        private val LOG = Logger.getLogger(DrcClientService::class.java.name)
    }

    fun readEnkelvoudigInformatieobject(enkelvoudigInformatieobjectUUID: UUID): EnkelvoudigInformatieObject =
        drcClient.enkelvoudigInformatieobjectRead(enkelvoudigInformatieobjectUUID)

    fun readEnkelvoudigInformatieobject(enkelvoudigInformatieobjectURI: URI): EnkelvoudigInformatieObject {
        validateZgwApiUri(enkelvoudigInformatieobjectURI, configurationService.readZgwApiClientMpRestUrl())
        return readEnkelvoudigInformatieobject(enkelvoudigInformatieobjectURI.extractUuid())
    }

    fun readEnkelvoudigInformatieobjectVersie(
        enkelvoudigInformatieobjectUUID: UUID,
        version: Int
    ): EnkelvoudigInformatieObject =
        drcClient.enkelvoudigInformatieobjectReadVersie(uuid = enkelvoudigInformatieobjectUUID, versie = version)

    fun deleteEnkelvoudigInformatieobject(enkelvoudigInformatieobjectUUID: UUID) {
        drcClient.enkelvoudigInformatieobjectDelete(enkelvoudigInformatieobjectUUID)
    }

    fun updateEnkelvoudigInformatieobject(
        enkelvoudigInformatieobjectUUID: UUID,
        enkelvoudigInformatieObjectWithLockRequest: EnkelvoudigInformatieObjectWithLockRequest,
        auditExplanation: String?
    ): EnkelvoudigInformatieObject {
        auditExplanation?.let { zgwClientHeadersFactory.setAuditExplanation(it) }
        return drcClient.enkelvoudigInformatieobjectPartialUpdate(
            uuid = enkelvoudigInformatieobjectUUID,
            enkelvoudigInformatieObjectWithLockRequest = enkelvoudigInformatieObjectWithLockRequest
        )
    }

    fun lockEnkelvoudigInformatieobject(enkelvoudigInformatieobjectUUID: UUID): String {
        return drcClient.enkelvoudigInformatieobjectLock(
            uuid = enkelvoudigInformatieobjectUUID,
            enkelvoudigInformatieObjectLock = LockEnkelvoudigInformatieObject(UUID.randomUUID().toString())
        ).lock
    }

    fun unlockEnkelvoudigInformatieobject(enkelvoudigInformatieobjectUUID: UUID, lock: String) {
        drcClient.enkelvoudigInformatieobjectUnlock(
            uuid = enkelvoudigInformatieobjectUUID,
            lock = LockEnkelvoudigInformatieObject(lock)
        )
    }

    /**
     * Streams the content of a document. The returned stream is not buffered in memory, so the
     * caller both has to consume it and to close it.
     */
    fun downloadEnkelvoudigInformatieobject(enkelvoudigInformatieobjectUUID: UUID): InputStream =
        drcClient.enkelvoudigInformatieobjectDownload(enkelvoudigInformatieobjectUUID).toContentStream {
            "Content of enkelvoudig informatieobject with uuid " +
                "'$enkelvoudigInformatieobjectUUID' could not be read."
        }

    /**
     * Streams the content of a specific version of a document. The returned stream is not buffered
     * in memory, so the caller both has to consume it and to close it.
     */
    fun downloadEnkelvoudigInformatieobjectVersie(
        enkelvoudigInformatieobjectUUID: UUID,
        version: Int
    ): InputStream =
        drcClient.enkelvoudigInformatieobjectDownloadVersie(
            uuid = enkelvoudigInformatieobjectUUID,
            versie = version
        ).toContentStream {
            "Content of enkelvoudig informatieobject with uuid '$enkelvoudigInformatieobjectUUID' " +
                "and version '$version' could not be read."
        }

    fun listAuditTrail(enkelvoudigInformatieobjectUUID: UUID): List<AuditTrailRegel> =
        drcClient.listAuditTrail(enkelvoudigInformatieobjectUUID)

    fun listEnkelvoudigInformatieObjecten(
        filter: EnkelvoudigInformatieobjectListParameters
    ): Results<EnkelvoudigInformatieObject> = drcClient.enkelvoudigInformatieobjectList(filter)

    fun createEnkelvoudigInformatieobject(
        enkelvoudigInformatieObjectCreateLockRequest: EnkelvoudigInformatieObjectCreateLockRequest
    ): EnkelvoudigInformatieObject = drcClient.enkelvoudigInformatieobjectCreate(
        enkelvoudigInformatieObjectCreateLockRequest
    )

    /**
     * Creates a document with the given [content].
     *
     * Content that fits in memory is sent base64 encoded in the create request itself. Larger
     * content is uploaded in parts: the document is created with only its `bestandsomvang`, which
     * makes the documents registry respond with the parts it expects, after which every part is
     * streamed to it and the document is unlocked.
     */
    fun createEnkelvoudigInformatieobject(
        enkelvoudigInformatieObjectCreateLockRequest: EnkelvoudigInformatieObjectCreateLockRequest,
        content: DocumentContent
    ): EnkelvoudigInformatieObject {
        enkelvoudigInformatieObjectCreateLockRequest.bestandsomvang = content.sizeInBytes.toInt()
        return if (content is InMemoryDocumentContent) {
            enkelvoudigInformatieObjectCreateLockRequest.inhoud = content.toByteArray().toBase64String()
            drcClient.enkelvoudigInformatieobjectCreate(enkelvoudigInformatieObjectCreateLockRequest)
        } else {
            enkelvoudigInformatieObjectCreateLockRequest.inhoud = null
            drcClient.enkelvoudigInformatieobjectCreateForPartsUpload(enkelvoudigInformatieObjectCreateLockRequest)
                .let { uploadInParts(createdDocument = it, content = content) }
        }
    }

    /**
     * Adds a new version of a document with the given [content], following the same two routes as
     * [createEnkelvoudigInformatieobject].
     *
     * The document is already locked by the caller, which also set that lock on
     * [enkelvoudigInformatieObjectWithLockRequest]. The parts route reuses that lock and leaves the
     * document locked: unlocking it is what commits the new version, and that is the caller's lock
     * to release.
     */
    fun updateEnkelvoudigInformatieobject(
        enkelvoudigInformatieobjectUUID: UUID,
        enkelvoudigInformatieObjectWithLockRequest: EnkelvoudigInformatieObjectWithLockRequest,
        auditExplanation: String?,
        content: DocumentContent
    ): EnkelvoudigInformatieObject {
        enkelvoudigInformatieObjectWithLockRequest.bestandsomvang = content.sizeInBytes.toInt()
        enkelvoudigInformatieObjectWithLockRequest.inhoud = when (content) {
            is InMemoryDocumentContent -> content.toByteArray().toBase64String()
            else -> null
        }
        val updatedDocument = updateEnkelvoudigInformatieobject(
            enkelvoudigInformatieobjectUUID = enkelvoudigInformatieobjectUUID,
            enkelvoudigInformatieObjectWithLockRequest = enkelvoudigInformatieObjectWithLockRequest,
            auditExplanation = auditExplanation
        )
        if (content is InMemoryDocumentContent) {
            return updatedDocument
        }
        uploadParts(
            documentUUID = enkelvoudigInformatieobjectUUID,
            parts = updatedDocument.bestandsdelen,
            lock = enkelvoudigInformatieObjectWithLockRequest.lock,
            content = content
        )
        return readEnkelvoudigInformatieobject(enkelvoudigInformatieobjectUUID)
    }

    fun createGebruiksrechten(gebruiksrechten: Gebruiksrechten) =
        drcClient.gebruiksrechtenCreate(gebruiksrechten)

    private fun uploadInParts(
        createdDocument: EnkelvoudigInformatieObjectCreateLockSub,
        content: DocumentContent
    ): EnkelvoudigInformatieObject {
        val documentUUID = createdDocument.url.extractUuid()
        val lock = createdDocument.lock
        var isUploaded = false
        try {
            uploadParts(
                documentUUID = documentUUID,
                parts = createdDocument.bestandsdelen,
                lock = lock,
                content = content
            )
            // creating a document with a bestandsomvang but without inhoud locks it; unlocking is what
            // makes the uploaded content the content of the document
            unlockEnkelvoudigInformatieobject(enkelvoudigInformatieobjectUUID = documentUUID, lock = lock)
            isUploaded = true
            return readEnkelvoudigInformatieobject(documentUUID)
        } finally {
            if (!isUploaded) {
                deleteDocumentWithIncompleteContent(documentUUID = documentUUID, lock = lock)
            }
        }
    }

    /**
     * Best effort, and deliberately silent: this runs while another failure is on its way out, and
     * must not replace it. A locked document cannot be deleted, so it is unlocked first.
     */
    private fun deleteDocumentWithIncompleteContent(documentUUID: UUID, lock: String) {
        LOG.warning { "Deleting document with uuid '$documentUUID' because uploading its content in parts failed" }
        logFailureToCleanUp(documentUUID) {
            drcClient.enkelvoudigInformatieobjectUnlock(
                uuid = documentUUID,
                lock = LockEnkelvoudigInformatieObject(lock)
            )
            drcClient.enkelvoudigInformatieobjectDelete(documentUUID)
        }
    }

    private fun logFailureToCleanUp(documentUUID: UUID, cleanUp: () -> Unit) {
        try {
            cleanUp()
        } catch (zgwRuntimeException: ZgwRuntimeException) {
            LOG.warning { "Failed to delete document with uuid '$documentUUID': ${zgwRuntimeException.message}" }
        } catch (zgwValidationErrorException: ZgwValidationErrorException) {
            LOG.warning { "Failed to delete document with uuid '$documentUUID': ${zgwValidationErrorException.message}" }
        } catch (webApplicationException: WebApplicationException) {
            LOG.warning { "Failed to delete document with uuid '$documentUUID': ${webApplicationException.message}" }
        } catch (processingException: ProcessingException) {
            LOG.warning { "Failed to delete document with uuid '$documentUUID': ${processingException.message}" }
        }
    }

    private fun uploadParts(
        documentUUID: UUID,
        parts: List<BestandsDeel>?,
        lock: String,
        content: DocumentContent
    ) {
        val orderedParts = parts.orEmpty().sortedBy(BestandsDeel::getVolgnummer)
        assertPartsCoverContent(documentUUID = documentUUID, parts = orderedParts, content = content)
        content.inputStream().use { contentStream ->
            orderedParts.forEach { part ->
                // the part travels straight from the content stream into the request, so however
                // large the documents registry announced it, it is never held on the heap
                val partContent = BestandsDeelInputStream(content = contentStream, sizeInBytes = part.omvang)
                val body = BestandsDeelMultipartBody(partContent = partContent, lock = lock)
                drcClient.bestandsdeelUpdate(
                    uuid = part.url.extractUuid(),
                    contentType = body.contentType,
                    bestandsDeel = body.inputStream()
                )
                if (partContent.bytesRead != part.omvang) {
                    throw DrcRuntimeException(
                        "Only ${partContent.bytesRead} of the ${part.omvang} bytes of bestandsdeel " +
                            "${part.volgnummer} of document with uuid '$documentUUID' could be read, so " +
                            "keeping the document would store it truncated."
                    )
                }
            }
        }
    }

    private fun assertPartsCoverContent(documentUUID: UUID, parts: List<BestandsDeel>, content: DocumentContent) {
        if (parts.isEmpty()) {
            throw DrcRuntimeException(
                "The documents registry announced no bestandsdelen for document with uuid '$documentUUID' of " +
                    "${content.sizeInBytes} bytes, so its content cannot be uploaded in parts."
            )
        }
        val announcedSizeInBytes = parts.sumOf { it.omvang.toLong() }
        if (announcedSizeInBytes != content.sizeInBytes) {
            throw DrcRuntimeException(
                "The documents registry announced bestandsdelen of $announcedSizeInBytes bytes in total for " +
                    "document with uuid '$documentUUID' of ${content.sizeInBytes} bytes, so uploading its " +
                    "content in parts would not store it in full."
            )
        }
    }

    /**
     * Hands out the entity of this response as a stream that also closes the response itself.
     *
     * The response is not read into memory, so it holds on to the connection for as long as the
     * stream is open. The caller only ever sees the stream, and so has no other way to release it.
     */
    private fun Response.toContentStream(errorMessage: () -> String): InputStream {
        var isHandedOver = false
        try {
            val entityStream = readEntity(InputStream::class.java) ?: throw DrcRuntimeException(errorMessage())
            return object : FilterInputStream(entityStream) {
                override fun close() {
                    try {
                        super.close()
                    } finally {
                        this@toContentStream.close()
                    }
                }
            }.also { isHandedOver = true }
        } finally {
            if (!isHandedOver) {
                close()
            }
        }
    }
}
