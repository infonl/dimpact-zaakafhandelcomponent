/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.detacheddocuments.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.zac.app.identity.converter.RestUserConverter
import nl.info.zac.app.model.createRESTUser
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocument
import nl.info.zac.document.detacheddocument.repository.model.createDetachedDocument
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.enkelvoudiginformatieobject.model.createEnkelvoudigInformatieObjectLock
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class RestDetachedDocumentConverterTest : BehaviorSpec({
    val userConverter = mockk<RestUserConverter>()
    val lockService = mockk<EnkelvoudigInformatieObjectLockService>()

    val converter = RestDetachedDocumentConverter(userConverter, lockService)

    afterEach {
        checkUnnecessaryStub()
    }

    given("a valid detached document and informatieobjectTypeUUID") {
        val uuid = UUID.randomUUID()
        val informatieobjectTypeUUID = UUID.randomUUID()
        val userId = "user-123"
        val detachedDocument = createDetachedDocument(
            uuid = uuid,
            userId = userId
        )

        val convertedUser = createRESTUser(id = userId)
        val lock = createEnkelvoudigInformatieObjectLock(
            lock = "LOCKED"
        )

        every { userConverter.convertUserId(userId) } returns convertedUser
        every { lockService.findLock(uuid) } returns lock

        `when`("convert is invoked") {
            val result = converter.convertToRestDetachedDocument(
                detachedDocument = detachedDocument,
                informatieobjectTypeUUID = informatieobjectTypeUUID,
                isZaakspecifiekGeautoriseerd = false
            )

            then("the result should have the expected values") {
                result.id shouldBe 1L
                result.documentUUID shouldBe uuid
                result.documentID shouldBe "DOC-456"
                result.informatieobjectTypeUUID shouldBe informatieobjectTypeUUID
                result.titel shouldBe "fakeTitel"
                result.zaakID shouldBe "ZAAK-001"
                result.creatiedatum shouldBe detachedDocument.creatiedatum
                result.bestandsnaam shouldBe "test.pdf"
                result.ontkoppeldDoor shouldBe convertedUser
                result.ontkoppeldOp shouldBe detachedDocument.ontkoppeldOp
                result.reden shouldBe "fakeReason"
                result.isVergrendeld shouldBe true
            }
        }
    }

    given("a detached document that is not locked") {
        val uuid = UUID.randomUUID()
        val userId = "user-456"
        val detachedDocument = DetachedDocument().apply {
            id = 2L
            documentUUID = uuid
            documentID = "DOC-789"
            ontkoppeldDoor = userId
            titel = "Zonder lock"
            bestandsnaam = "bestand.pdf"
            zaakID = "ZAAK-999"
            creatiedatum = LocalDate.now()
            ontkoppeldOp = ZonedDateTime.now()
            reden = "Geen lock"
        }

        val convertedUser = createRESTUser(id = userId)

        every { userConverter.convertUserId(userId) } returns convertedUser
        every { lockService.findLock(uuid) } returns null

        `when`("convert is invoked") {
            val result = converter.convertToRestDetachedDocument(
                detachedDocument = detachedDocument,
                informatieobjectTypeUUID = UUID.randomUUID(),
                isZaakspecifiekGeautoriseerd = false
            )

            then("isVergrendeld should be false in the result") {
                result.isVergrendeld shouldBe false
            }
        }
    }

    given("two detached documents of which only the zaak of the first is zaakspecifiek geautoriseerd") {
        val zaakspecifiekGeautoriseerdeZaakIdentificatie = "fakeZaakIdentificatie1"
        val otherZaakIdentificatie = "fakeZaakIdentificatie2"
        val firstDetachedDocument = createDetachedDocument().apply {
            zaakID = zaakspecifiekGeautoriseerdeZaakIdentificatie
        }
        val secondDetachedDocument = createDetachedDocument().apply { zaakID = otherZaakIdentificatie }
        val firstInformatieobjectTypeUUID = UUID.randomUUID()
        val secondInformatieobjectTypeUUID = UUID.randomUUID()

        every { userConverter.convertUserId("fakeUserId") } returns createRESTUser(id = "fakeUserId")
        every { lockService.findLock(firstDetachedDocument.documentUUID) } returns null
        every { lockService.findLock(secondDetachedDocument.documentUUID) } returns null

        `when`("the detached documents are converted with the zaakspecifiek geautoriseerde zaaknummers") {
            val restDetachedDocuments = converter.convertToRestDetachedDocuments(
                documenten = listOf(firstDetachedDocument, secondDetachedDocument),
                informatieobjectTypeUUIDs = listOf(firstInformatieobjectTypeUUID, secondInformatieobjectTypeUUID),
                zaakspecifiekGeautoriseerdeZaakIDs = setOf(zaakspecifiekGeautoriseerdeZaakIdentificatie)
            )

            then("only the document of the zaakspecifiek geautoriseerde zaak is marked as such") {
                restDetachedDocuments.map { it.zaakID to it.isZaakspecifiekGeautoriseerd } shouldBe listOf(
                    zaakspecifiekGeautoriseerdeZaakIdentificatie to true,
                    otherZaakIdentificatie to false
                )
            }

            and("each document keeps its own informatieobjecttype") {
                restDetachedDocuments.map { it.informatieobjectTypeUUID } shouldBe listOf(
                    firstInformatieobjectTypeUUID,
                    secondInformatieobjectTypeUUID
                )
            }
        }
    }
})
