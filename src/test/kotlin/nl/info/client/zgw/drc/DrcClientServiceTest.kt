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
import io.kotest.matchers.string.shouldContain
import nl.info.client.zgw.drc.model.createBestandsDeel
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObjectCreateLockRequest
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObjectCreateLockSub
import nl.info.client.zgw.drc.model.generated.BestandsDeel
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectCreateLockRequest
import nl.info.client.zgw.util.extractUuid
import nl.info.zac.document.content.InMemoryDocumentContent
import nl.info.zac.document.content.TemporaryFileDocumentContent
import java.net.URI
import java.nio.file.Files
import java.util.Base64
import java.io.ByteArrayInputStream
import java.io.InputStream
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

        `when`("downloadEnkelvoudigInformatieobject is called") {
            val content = ByteArrayInputStream("fakeContent".toByteArray())
            val response = mockk<Response>()
            every { response.readEntity(InputStream::class.java) } returns content
            every { response.close() } just runs
            every { drcClient.enkelvoudigInformatieobjectDownload(uuid) } returns response

            val result = drcClientService.downloadEnkelvoudigInformatieobject(uuid)

            then(
                "it returns the content stream without buffering it in memory first, and releases the " +
                    "response only once the caller closes that stream"
            ) {
                result.readBytes() shouldBe "fakeContent".toByteArray()
                verify(exactly = 0) { response.bufferEntity() }
                verify(exactly = 0) { response.close() }

                result.close()

                verify(exactly = 1) { response.close() }
            }
        }

        `when`("the response has no entity and downloadEnkelvoudigInformatieobject is called") {
            val response = mockk<Response>()
            every { response.readEntity(InputStream::class.java) } returns null
            every { response.close() } just runs
            every { drcClient.enkelvoudigInformatieobjectDownload(uuid) } returns response

            val drcRuntimeException = shouldThrow<DrcRuntimeException> {
                drcClientService.downloadEnkelvoudigInformatieobject(uuid)
            }

            then(
                "it should throw a DrcRuntimeException and release the response, because no stream reaches " +
                    "the caller that could release it"
            ) {
                drcRuntimeException.message shouldBe
                    "Content of enkelvoudig informatieobject with uuid '$uuid' could not be read."
                verify(exactly = 1) { response.close() }
            }
        }
    }
    given("A document that fits in memory") {
        val content = InMemoryDocumentContent("fakeContent".toByteArray())
        val createRequest = createEnkelvoudigInformatieObjectCreateLockRequest()
        val createdDocument = createEnkelvoudigInformatieObject()
        val requestSlot = slot<EnkelvoudigInformatieObjectCreateLockRequest>()

        every { drcClient.enkelvoudigInformatieobjectCreate(capture(requestSlot)) } returns createdDocument

        `when`("it is created") {
            val result = drcClientService.createEnkelvoudigInformatieobject(createRequest, content)

            then("its content is sent base64 encoded in the create request itself") {
                result shouldBe createdDocument
                requestSlot.captured.inhoud shouldBe
                    Base64.getEncoder().encodeToString("fakeContent".toByteArray())
                requestSlot.captured.bestandsomvang shouldBe "fakeContent".toByteArray().size
            }
        }
    }

    given("A document too large to fit in memory") {
        val bytes = "0123456789".toByteArray()
        val documentUUID = UUID.randomUUID()
        val documentUrl = URI("https://example.com/enkelvoudiginformatieobjecten/$documentUUID")
        val firstPart = createBestandsDeel(volgnummer = 1, omvang = 4, lock = "fakeLock")
        val secondPart = createBestandsDeel(volgnummer = 2, omvang = 6, lock = "fakeLock")
        val temporaryFile = Files.createTempFile("fakeDocument", null).also { Files.write(it, bytes) }
        val content = TemporaryFileDocumentContent(temporaryFile)
        val createRequest = createEnkelvoudigInformatieObjectCreateLockRequest()
        // the parts are deliberately announced out of order to prove that they are uploaded by volgnummer
        val createdDocument = createEnkelvoudigInformatieObjectCreateLockSub(
            uuid = documentUUID,
            url = documentUrl,
            bestandsdelen = listOf(secondPart, firstPart)
        )
        val completedDocument = createEnkelvoudigInformatieObject(uuid = documentUUID, url = documentUrl)
        val requestSlot = slot<EnkelvoudigInformatieObjectCreateLockRequest>()
        val uploadedParts = mutableListOf<Pair<UUID, ByteArray>>()
        val uploadedContentTypes = mutableListOf<String>()

        every {
            drcClient.enkelvoudigInformatieobjectCreateForPartsUpload(capture(requestSlot))
        } returns createdDocument
        every { drcClient.bestandsdeelUpdate(any(), any(), any()) } answers {
            uploadedContentTypes.add(secondArg())
            uploadedParts.add(firstArg<UUID>() to thirdArg<ByteArray>())
            createBestandsDeel()
        }
        val unlockSlot = slot<LockEnkelvoudigInformatieObject>()
        every {
            drcClient.enkelvoudigInformatieobjectUnlock(documentUUID, capture(unlockSlot))
        } returns mockk()
        every { drcClient.enkelvoudigInformatieobjectRead(documentUUID) } returns completedDocument

        `when`("it is created") {
            val result = drcClientService.createEnkelvoudigInformatieobject(createRequest, content)

            then("it is created without content and every part is streamed in order") {
                requestSlot.captured.inhoud shouldBe null
                requestSlot.captured.bestandsomvang shouldBe bytes.size
                uploadedParts.map { it.first } shouldBe listOf(
                    firstPart.url.extractUuid(),
                    secondPart.url.extractUuid()
                )
                uploadedParts.map { String(it.second) }.forEachIndexed { index, body ->
                    body shouldContain listOf("0123", "456789")[index]
                    body shouldContain """name="lock""""
                    body shouldContain "fakeLock"
                }
                uploadedContentTypes.forEach { it shouldContain "multipart/form-data; boundary=" }
            }

            and("the document is unlocked so that the uploaded content becomes its content") {
                result shouldBe completedDocument
                unlockSlot.captured.lock shouldBe "fakeLock"
            }
        }

        content.close()
    }

    given("A document too large to fit in memory whose parts cannot be uploaded") {
        val bytes = "0123456789".toByteArray()
        val documentUUID = UUID.randomUUID()
        val documentUrl = URI("https://example.com/enkelvoudiginformatieobjecten/$documentUUID")
        val temporaryFile = Files.createTempFile("fakeDocument", null).also { Files.write(it, bytes) }
        val content = TemporaryFileDocumentContent(temporaryFile)
        val createdDocument = createEnkelvoudigInformatieObjectCreateLockSub(
            uuid = documentUUID,
            url = documentUrl,
            bestandsdelen = listOf(createBestandsDeel(volgnummer = 1, omvang = 10))
        )

        every { drcClient.enkelvoudigInformatieobjectCreateForPartsUpload(any()) } returns createdDocument
        every {
            drcClient.bestandsdeelUpdate(any(), any(), any())
        } throws DrcRuntimeException("fake upload failure")
        every { drcClient.enkelvoudigInformatieobjectUnlock(documentUUID, any()) } returns mockk()
        every { drcClient.enkelvoudigInformatieobjectDelete(documentUUID) } returns mockk()

        `when`("it is created") {
            val drcRuntimeException = shouldThrow<DrcRuntimeException> {
                drcClientService.createEnkelvoudigInformatieobject(
                    createEnkelvoudigInformatieObjectCreateLockRequest(),
                    content
                )
            }

            then("the failure surfaces and no document with missing content is left behind") {
                drcRuntimeException.message shouldBe "fake upload failure"
                verify(exactly = 1) { drcClient.enkelvoudigInformatieobjectDelete(documentUUID) }
            }
        }

        content.close()
    }

    given("A document too large to fit in memory for which no parts are announced") {
        val temporaryFile = Files.createTempFile("fakeDocument", null)
            .also { Files.write(it, "0123456789".toByteArray()) }
        val content = TemporaryFileDocumentContent(temporaryFile)
        val documentUUID = UUID.randomUUID()
        val documentUrl = URI("https://example.com/enkelvoudiginformatieobjecten/$documentUUID")

        every {
            drcClient.enkelvoudigInformatieobjectCreateForPartsUpload(any())
        } returns createEnkelvoudigInformatieObjectCreateLockSub(
            uuid = documentUUID,
            url = documentUrl,
            bestandsdelen = emptyList()
        )
        every { drcClient.enkelvoudigInformatieobjectUnlock(documentUUID, any()) } returns mockk()
        every { drcClient.enkelvoudigInformatieobjectDelete(documentUUID) } returns mockk()

        `when`("it is created") {
            val drcRuntimeException = shouldThrow<DrcRuntimeException> {
                drcClientService.createEnkelvoudigInformatieobject(
                    createEnkelvoudigInformatieObjectCreateLockRequest(),
                    content
                )
            }

            then("the documents registry is reported as not supporting uploads in parts") {
                drcRuntimeException.message shouldContain "announced no bestandsdelen"
            }
        }

        content.close()
    }

    given("A document too large to fit in memory for which parts are announced that do not cover it") {
        val temporaryFile = Files.createTempFile("fakeDocument", null)
            .also { Files.write(it, "0123456789".toByteArray()) }
        val content = TemporaryFileDocumentContent(temporaryFile)
        val documentUUID = UUID.randomUUID()
        val documentUrl = URI("https://example.com/enkelvoudiginformatieobjecten/$documentUUID")

        every {
            drcClient.enkelvoudigInformatieobjectCreateForPartsUpload(any())
        } returns createEnkelvoudigInformatieObjectCreateLockSub(
            uuid = documentUUID,
            url = documentUrl,
            bestandsdelen = listOf(
                createBestandsDeel(volgnummer = 1, omvang = 4),
                createBestandsDeel(volgnummer = 2, omvang = 4)
            )
        )
        every { drcClient.enkelvoudigInformatieobjectUnlock(documentUUID, any()) } returns mockk()
        every { drcClient.enkelvoudigInformatieobjectDelete(documentUUID) } returns mockk()

        `when`("it is created") {
            val drcRuntimeException = shouldThrow<DrcRuntimeException> {
                drcClientService.createEnkelvoudigInformatieobject(
                    createEnkelvoudigInformatieObjectCreateLockRequest(),
                    content
                )
            }

            then("nothing is uploaded, so that a document is never silently stored truncated") {
                drcRuntimeException.message shouldContain "bestandsdelen of 8 bytes in total"
                verify(exactly = 0) { drcClient.bestandsdeelUpdate(any(), any(), any()) }
                verify(exactly = 1) { drcClient.enkelvoudigInformatieobjectDelete(documentUUID) }
            }
        }

        content.close()
    }

    given("a document whose content stream yields fewer bytes than it reports") {
        val documentUUID = UUID.randomUUID()
        val documentUrl = URI("https://example.com/enkelvoudiginformatieobjecten/$documentUUID")
        val temporaryFile = Files.createTempFile("fakeDocument", null)
            .also { Files.write(it, "0123456789".toByteArray()) }
        // the size is captured when the content is constructed, so truncating the file afterwards
        // leaves content that reports more bytes than its stream can yield
        val content = TemporaryFileDocumentContent(temporaryFile)
        Files.write(temporaryFile, "0123".toByteArray())

        every {
            drcClient.enkelvoudigInformatieobjectCreateForPartsUpload(any())
        } returns createEnkelvoudigInformatieObjectCreateLockSub(
            uuid = documentUUID,
            url = documentUrl,
            bestandsdelen = listOf(createBestandsDeel(volgnummer = 1, omvang = 10))
        )
        every { drcClient.enkelvoudigInformatieobjectUnlock(documentUUID, any()) } returns mockk()
        every { drcClient.enkelvoudigInformatieobjectDelete(documentUUID) } returns mockk()

        `when`("it is created") {
            val drcRuntimeException = shouldThrow<DrcRuntimeException> {
                drcClientService.createEnkelvoudigInformatieobject(
                    createEnkelvoudigInformatieObjectCreateLockRequest(),
                    content
                )
            }

            then("the short part is not uploaded, so that the document is never stored truncated") {
                drcRuntimeException.message shouldContain "Only 4 of the 10 bytes of bestandsdeel 1"
                verify(exactly = 0) { drcClient.bestandsdeelUpdate(any(), any(), any()) }
                verify(exactly = 1) { drcClient.enkelvoudigInformatieobjectDelete(documentUUID) }
            }
        }

        content.close()
    }
})
