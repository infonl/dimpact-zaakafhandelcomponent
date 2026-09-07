/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.drc

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.ProcessingException
import nl.info.client.zgw.shared.model.Results
import nl.info.client.zgw.shared.model.audit.AuditTrailRegel
import nl.info.client.zgw.drc.exception.DrcRuntimeException
import nl.info.client.zgw.drc.model.BestandsDeelUploadRequest
import nl.info.client.zgw.drc.model.EnkelvoudigInformatieobjectListParameters
import nl.info.client.zgw.drc.model.generated.BestandsDeel
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectCreateLockRequest
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
        drcClient.enkelvoudigInformatieobjectDownload(enkelvoudigInformatieobjectUUID).let { response ->
            response.readEntity(InputStream::class.java)
                ?: throw DrcRuntimeException(
                    "Content of enkelvoudig informatieobject with uuid " +
                        "'$enkelvoudigInformatieobjectUUID' could not be read."
                )
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
        ).let { response ->
            response.readEntity(InputStream::class.java)
                ?: throw DrcRuntimeException(
                    "Content of enkelvoudig informatieobject with uuid '$enkelvoudigInformatieobjectUUID' " +
                        "and version '$version' could not be read."
                )
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
            drcClient.enkelvoudigInformatieobjectCreate(enkelvoudigInformatieObjectCreateLockRequest)
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
        uploadParts(documentUUID = enkelvoudigInformatieobjectUUID, document = updatedDocument, content = content)
        return readEnkelvoudigInformatieobject(enkelvoudigInformatieobjectUUID)
    }

    fun createGebruiksrechten(gebruiksrechten: Gebruiksrechten) =
        drcClient.gebruiksrechtenCreate(gebruiksrechten)

    private fun uploadInParts(
        createdDocument: EnkelvoudigInformatieObject,
        content: DocumentContent
    ): EnkelvoudigInformatieObject {
        val documentUUID = createdDocument.url.extractUuid()
        var isUploaded = false
        try {
            val lock = uploadParts(documentUUID = documentUUID, document = createdDocument, content = content)
            // creating a document with a bestandsomvang but without inhoud locks it; unlocking is what
            // makes the uploaded content the content of the document
            unlockEnkelvoudigInformatieobject(enkelvoudigInformatieobjectUUID = documentUUID, lock = lock)
            isUploaded = true
            return readEnkelvoudigInformatieobject(documentUUID)
        } finally {
            if (!isUploaded) {
                deleteDocumentWithIncompleteContent(documentUUID)
            }
        }
    }

    /**
     * Best effort: a failure here must not hide the failure that caused it.
     */
    private fun deleteDocumentWithIncompleteContent(documentUUID: UUID) {
        LOG.warning { "Deleting document with uuid '$documentUUID' because uploading its content in parts failed" }
        try {
            drcClient.enkelvoudigInformatieobjectDelete(documentUUID)
        } catch (drcRuntimeException: DrcRuntimeException) {
            LOG.warning { "Failed to delete document with uuid '$documentUUID': ${drcRuntimeException.message}" }
        } catch (processingException: ProcessingException) {
            LOG.warning { "Failed to delete document with uuid '$documentUUID': ${processingException.message}" }
        }
    }

    private fun uploadParts(
        documentUUID: UUID,
        document: EnkelvoudigInformatieObject,
        content: DocumentContent
    ): String {
        val parts = document.bestandsdelen.orEmpty().sortedBy(BestandsDeel::getVolgnummer)
        if (parts.isEmpty()) {
            throw DrcRuntimeException(
                "The documents registry announced no bestandsdelen for document with uuid '$documentUUID' of " +
                    "${content.sizeInBytes} bytes, so its content cannot be uploaded in parts."
            )
        }
        val lock = parts.first().lock
            ?: throw DrcRuntimeException(
                "The documents registry announced bestandsdelen without a lock for document with uuid '$documentUUID'."
            )
        content.inputStream().use { contentStream ->
            parts.forEach { part ->
                drcClient.bestandsdeelUpdate(
                    uuid = part.url.extractUuid(),
                    bestandsDeelUploadRequest = BestandsDeelUploadRequest(
                        inhoud = BoundedInputStream(contentStream, part.omvang.toLong()),
                        lock = lock
                    )
                )
            }
        }
        return lock
    }
}

/**
 * Exposes at most [limit] bytes of [delegate] as a stream of its own, so that consecutive parts can
 * be read from a single stream over the document without slicing it into byte arrays first.
 * Closing it deliberately does not close [delegate], which the next part still needs.
 */
private class BoundedInputStream(private val delegate: InputStream, private val limit: Long) : InputStream() {
    private var bytesRead = 0L

    override fun read(): Int {
        if (bytesRead >= limit) return -1
        return delegate.read().also { if (it != -1) bytesRead++ }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (bytesRead >= limit) return -1
        val allowed = minOf(length.toLong(), limit - bytesRead).toInt()
        return delegate.read(buffer, offset, allowed).also { if (it != -1) bytesRead += it }
    }

    override fun available() = minOf(delegate.available().toLong(), limit - bytesRead).toInt()

    override fun close() {
        // the delegate is shared between parts and is closed by whoever opened it
    }
}
