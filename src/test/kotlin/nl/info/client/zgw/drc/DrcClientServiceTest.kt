/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.drc

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import jakarta.ws.rs.core.Response
import nl.info.client.zgw.drc.exception.DrcRuntimeException
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObjectWithLockRequest
import nl.info.client.zgw.drc.model.createLockEnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.LockEnkelvoudigInformatieObject
import nl.info.client.zgw.util.ZgwClientHeadersFactory
import nl.info.zac.configuration.ConfigurationService
import java.io.ByteArrayInputStream
import java.util.UUID

class DrcClientServiceTest : BehaviorSpec({
    val drcClient = mockk<DrcClient>()
    val zgwClientHeadersFactory = mockk<ZgwClientHeadersFactory>()
    val configurationService = mockk<ConfigurationService>()
    val drcClientService = DrcClientService(
        drcClient,
        zgwClientHeadersFactory,
        configurationService
    )

    afterEach {
        checkUnnecessaryStub()
        clearAllMocks()
    }

    given("A valid UUID for an EnkelvoudigInformatieobject") {
        val enkelvoudigInformatieobjectUUID = UUID.randomUUID()
        val lock = "fakeLock"
        val lockEnkelvoudigInformatieObject = createLockEnkelvoudigInformatieObject(lock = lock)
        val lockEnkelvoudigInformatieObjectSlot = slot<LockEnkelvoudigInformatieObject>()
        every {
            drcClient.enkelvoudigInformatieobjectLock(
                enkelvoudigInformatieobjectUUID,
                capture(lockEnkelvoudigInformatieObjectSlot)
            )
        } returns lockEnkelvoudigInformatieObject

        `when`("locking the EnkelvoudigInformatieobject") {
            val result = drcClientService.lockEnkelvoudigInformatieobject(enkelvoudigInformatieobjectUUID)

            then(
                """
                it should call the DRC client with a LockEnkelvoudigInformatieObject instance with a lock string
                generated from a (random) UUID and return the lock ID
                """
            ) {
                UUID.fromString(lockEnkelvoudigInformatieObjectSlot.captured.lock).shouldBeInstanceOf<UUID>()
                result shouldBe lock
            }
        }
    }

    given("An EnkelvoudigInformatieobject UUID and a patch request with a non-null audit explanation") {
        val uuid = UUID.randomUUID()
        val patchRequest = createEnkelvoudigInformatieObjectWithLockRequest()
        val updatedDocument = createEnkelvoudigInformatieObject(uuid = uuid)
        val auditExplanation = "some audit reason"
        every { zgwClientHeadersFactory.setAuditExplanation(auditExplanation) } just runs
        every {
            drcClient.enkelvoudigInformatieobjectPartialUpdate(
                uuid = uuid,
                enkelvoudigInformatieObjectWithLockRequest = patchRequest
            )
        } returns updatedDocument

        `when`("updating the EnkelvoudigInformatieobject") {
            val result = drcClientService.updateEnkelvoudigInformatieobject(
                enkelvoudigInformatieobjectUUID = uuid,
                enkelvoudigInformatieObjectWithLockRequest = patchRequest,
                auditExplanation = auditExplanation
            )

            then("it should set the audit explanation and return the updated document") {
                verify(exactly = 1) { zgwClientHeadersFactory.setAuditExplanation(auditExplanation) }
                result shouldBe updatedDocument
            }
        }
    }

    given("An EnkelvoudigInformatieobject UUID and a patch request with a null audit explanation") {
        val uuid = UUID.randomUUID()
        val patchRequest = createEnkelvoudigInformatieObjectWithLockRequest()
        val updatedDocument = createEnkelvoudigInformatieObject(uuid = uuid)
        every {
            drcClient.enkelvoudigInformatieobjectPartialUpdate(
                uuid = uuid,
                enkelvoudigInformatieObjectWithLockRequest = patchRequest
            )
        } returns updatedDocument

        `when`("updating the EnkelvoudigInformatieobject") {
            val result = drcClientService.updateEnkelvoudigInformatieobject(
                enkelvoudigInformatieobjectUUID = uuid,
                enkelvoudigInformatieObjectWithLockRequest = patchRequest,
                auditExplanation = null
            )

            then("it should NOT call setAuditExplanation and should return the updated document") {
                verify(exactly = 0) { zgwClientHeadersFactory.setAuditExplanation(any()) }
                result shouldBe updatedDocument
            }
        }
    }

    given("An EnkelvoudigInformatieobject UUID for download") {
        val uuid = UUID.randomUUID()

        `when`("the entity can be buffered and downloadEnkelvoudigInformatieobject is called") {
            then("it should return the content stream") {
                val content = ByteArrayInputStream("fakeContent".toByteArray())
                val response = mockk<Response>()
                every { response.bufferEntity() } returns true
                every { response.entity } returns content
                every { drcClient.enkelvoudigInformatieobjectDownload(uuid) } returns response

                val result = drcClientService.downloadEnkelvoudigInformatieobject(uuid)

                result shouldBe content
            }
        }

        `when`("the entity cannot be buffered and downloadEnkelvoudigInformatieobject is called") {
            then("it should throw a DrcRuntimeException") {
                val response = mockk<Response>()
                every { response.bufferEntity() } returns false
                every { drcClient.enkelvoudigInformatieobjectDownload(uuid) } returns response

                shouldThrow<DrcRuntimeException> {
                    drcClientService.downloadEnkelvoudigInformatieobject(uuid)
                }
            }
        }
    }
})
