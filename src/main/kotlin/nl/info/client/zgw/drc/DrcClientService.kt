/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.drc

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import net.atos.client.zgw.shared.model.Results
import net.atos.client.zgw.shared.model.audit.AuditTrailRegel
import nl.info.client.zgw.drc.exception.DrcRuntimeException
import nl.info.client.zgw.drc.model.EnkelvoudigInformatieobjectListParameters
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectCreateLockRequest
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectWithLockRequest
import nl.info.client.zgw.drc.model.generated.Gebruiksrechten
import nl.info.client.zgw.drc.model.generated.LockEnkelvoudigInformatieObject
import nl.info.client.zgw.util.ZgwClientHeadersFactory
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.util.validateZgwApiUri
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.io.ByteArrayInputStream
import java.net.URI
import java.util.UUID

@ApplicationScoped
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions")
class DrcClientService @Inject constructor(
    @RestClient private val drcClient: DrcClient,
    private val zgwClientHeadersFactory: ZgwClientHeadersFactory,
    private val configurationService: ConfigurationService
) {
    /**
     * Read [EnkelvoudigInformatieObject] via its UUID.
     * Throws a RuntimeException if the [EnkelvoudigInformatieObject] can not be read.
     *
     * @param uuid UUID of the [EnkelvoudigInformatieObject].
     * @return [EnkelvoudigInformatieObject]. Never 'null'!
     */
    fun readEnkelvoudigInformatieobject(uuid: UUID): EnkelvoudigInformatieObject =
        drcClient.enkelvoudigInformatieobjectRead(uuid)

    /**
     * Read [EnkelvoudigInformatieObject] via its URI.
     * Throws a RuntimeException if the [EnkelvoudigInformatieObject] can not be read.
     *
     * @param enkelvoudigInformatieobjectURI URI of the [EnkelvoudigInformatieObject].
     * @return [EnkelvoudigInformatieObject]. Never 'null'!
     */
    fun readEnkelvoudigInformatieobject(enkelvoudigInformatieobjectURI: URI): EnkelvoudigInformatieObject {
        validateZgwApiUri(enkelvoudigInformatieobjectURI, configurationService.readZgwApiClientMpRestUrl())
        return readEnkelvoudigInformatieobject(enkelvoudigInformatieobjectURI.extractUuid())
    }

    /**
     * Read [EnkelvoudigInformatieObject] via its UUID and version.
     * Throws a RuntimeException if the [EnkelvoudigInformatieObject] can not be read.
     *
     * @param uuid   UUID of the [EnkelvoudigInformatieObject].
     * @param versie Required version
     * @return [EnkelvoudigInformatieObject]. Never 'null'!
     */
    fun readEnkelvoudigInformatieobjectVersie(uuid: UUID, versie: Int): EnkelvoudigInformatieObject =
        drcClient.enkelvoudigInformatieobjectReadVersie(uuid = uuid, versie = versie)

    /**
     * DELETE [EnkelvoudigInformatieObject] via its UUID.
     * Throws a RuntimeException if the [EnkelvoudigInformatieObject] can not be deleted.
     *
     * @param uuid UUID of the [EnkelvoudigInformatieObject].
     */
    fun deleteEnkelvoudigInformatieobject(uuid: UUID) {
        drcClient.enkelvoudigInformatieobjectDelete(uuid)
    }

    fun updateEnkelvoudigInformatieobject(
        uuid: UUID,
        enkelvoudigInformatieObjectWithLockRequest: EnkelvoudigInformatieObjectWithLockRequest,
        auditExplanation: String?
    ): EnkelvoudigInformatieObject {
        auditExplanation?.let { zgwClientHeadersFactory.setAuditExplanation(it) }
        return drcClient.enkelvoudigInformatieobjectPartialUpdate(
            uuid = uuid,
            enkelvoudigInformatieObjectWithLockRequest = enkelvoudigInformatieObjectWithLockRequest
        )
    }

    /**
     * Lock a [EnkelvoudigInformatieObject].
     *
     * @param enkelvoudigInformatieobjectUUID [EnkelvoudigInformatieObject]
     */
    fun lockEnkelvoudigInformatieobject(enkelvoudigInformatieobjectUUID: UUID): String {
        // If the EnkelvoudigInformatieobject is already locked, a ValidationException is thrown.
        return drcClient.enkelvoudigInformatieobjectLock(
            uuid = enkelvoudigInformatieobjectUUID,
            enkelvoudigInformatieObjectLock = LockEnkelvoudigInformatieObject(UUID.randomUUID().toString())
        ).lock
    }

    /**
     * Unlock a [EnkelvoudigInformatieObject].
     *
     * @param enkelvoudigInformatieobjectUUID [EnkelvoudigInformatieObject]
     * @param lock                            The lock id
     */
    fun unlockEnkelvoudigInformatieobject(enkelvoudigInformatieobjectUUID: UUID, lock: String) {
        drcClient.enkelvoudigInformatieobjectUnlock(
            uuid = enkelvoudigInformatieobjectUUID,
            lock = LockEnkelvoudigInformatieObject(lock)
        )
    }

    /**
     * Download content of [EnkelvoudigInformatieObject].
     *
     * @param enkelvoudigInformatieobjectUUID UUID of [EnkelvoudigInformatieObject]
     * @return Content of [EnkelvoudigInformatieObject].
     */
    fun downloadEnkelvoudigInformatieobject(enkelvoudigInformatieobjectUUID: UUID): ByteArrayInputStream {
        val response = drcClient.enkelvoudigInformatieobjectDownload(enkelvoudigInformatieobjectUUID)
        if (!response.bufferEntity()) {
            throw DrcRuntimeException(
                "Content of enkelvoudig informatieobject with uuid '$enkelvoudigInformatieobjectUUID' could not be buffered."
            )
        }
        return response.entity as ByteArrayInputStream
    }

    /**
     * Download content of [EnkelvoudigInformatieObject] of a specific version
     *
     * @param enkelvoudigInformatieobjectUUID UUID of [EnkelvoudigInformatieObject]
     * @param versie                          Required version
     * @return Content of [EnkelvoudigInformatieObject].
     */
    fun downloadEnkelvoudigInformatieobjectVersie(
        enkelvoudigInformatieobjectUUID: UUID,
        versie: Int?
    ): ByteArrayInputStream {
        val response = drcClient.enkelvoudigInformatieobjectDownloadVersie(
            uuid = enkelvoudigInformatieobjectUUID,
            versie = versie
        )
        if (!response.bufferEntity()) {
            throw DrcRuntimeException(
                "Content of enkelvoudig informatieobject with uuid '$enkelvoudigInformatieobjectUUID' " +
                    "and version '$versie' could not be buffered."
            )
        }
        return response.entity as ByteArrayInputStream
    }

    /**
     * List all instances of [AuditTrailRegel] for a specific [EnkelvoudigInformatieObject].
     *
     * @param enkelvoudigInformatieobjectUUID UUID of [EnkelvoudigInformatieObject].
     * @return List of [AuditTrailRegel] instances.
     */
    fun listAuditTrail(enkelvoudigInformatieobjectUUID: UUID): List<AuditTrailRegel> =
        drcClient.listAuditTrail(enkelvoudigInformatieobjectUUID)

    /**
     * List instances of [EnkelvoudigInformatieObject] filtered by [EnkelvoudigInformatieobjectListParameters].
     *
     * @param filter [EnkelvoudigInformatieobjectListParameters].
     * @return List of [EnkelvoudigInformatieObject] instances.
     */
    fun listEnkelvoudigInformatieObjecten(
        filter: EnkelvoudigInformatieobjectListParameters
    ): Results<EnkelvoudigInformatieObject> = drcClient.enkelvoudigInformatieobjectList(filter)

    fun createEnkelvoudigInformatieobject(
        enkelvoudigInformatieObjectCreateLockRequest: EnkelvoudigInformatieObjectCreateLockRequest
    ): EnkelvoudigInformatieObject = drcClient.enkelvoudigInformatieobjectCreate(
        enkelvoudigInformatieObjectCreateLockRequest
    )

    fun createGebruiksrechten(gebruiksrechten: Gebruiksrechten) {
        drcClient.gebruiksrechtenCreate(gebruiksrechten)
    }
}
