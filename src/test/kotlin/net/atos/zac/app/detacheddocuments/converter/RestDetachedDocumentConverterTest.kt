/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.detacheddocuments.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.zac.app.identity.converter.RestUserConverter
import nl.info.zac.app.model.createRESTUser
import nl.info.zac.document.detacheddocument.model.DetachedDocument
import nl.info.zac.document.detacheddocument.model.createDetachedDocument
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.enkelvoudiginformatieobject.model.createEnkelvoudigInformatieObjectLock
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class RestDetachedDocumentConverterTest : BehaviorSpec({
    val userConverter = mockk<RestUserConverter>()
    val lockService = mockk<EnkelvoudigInformatieObjectLockService>()

    val converter = RestDetachedDocumentConverter(userConverter, lockService)

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("a valid detached document and informatieobjectTypeUUID") {
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

        When("convert is invoked") {
            val result = converter.convert(detachedDocument, informatieobjectTypeUUID)

            Then("the result should have the expected values") {
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

    Given("a detached document that is not locked") {
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

        When("convert is invoked") {
            val result = converter.convert(detachedDocument, UUID.randomUUID())

            Then("isVergrendeld should be false in the result") {
                result.isVergrendeld shouldBe false
            }
        }
    }
})
