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

    fun downloadEnkelvoudigInformatieobject(enkelvoudigInformatieobjectUUID: UUID): ByteArrayInputStream {
        val response = drcClient.enkelvoudigInformatieobjectDownload(enkelvoudigInformatieobjectUUID)
        if (!response.bufferEntity()) {
            throw DrcRuntimeException(
                "Content of enkelvoudig informatieobject with uuid '$enkelvoudigInformatieobjectUUID' could not be buffered."
            )
        }
        return response.entity as ByteArrayInputStream
    }

    fun downloadEnkelvoudigInformatieobjectVersie(
        enkelvoudigInformatieobjectUUID: UUID,
        version: Int
    ): ByteArrayInputStream {
        val response = drcClient.enkelvoudigInformatieobjectDownloadVersie(
            uuid = enkelvoudigInformatieobjectUUID,
            versie = version
        )
        if (!response.bufferEntity()) {
            throw DrcRuntimeException(
                "Content of enkelvoudig informatieobject with uuid '$enkelvoudigInformatieobjectUUID' " +
                    "and version '$version' could not be buffered."
            )
        }
        return response.entity as ByteArrayInputStream
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

    fun createGebruiksrechten(gebruiksrechten: Gebruiksrechten) {
        drcClient.gebruiksrechtenCreate(gebruiksrechten)
    }
}
