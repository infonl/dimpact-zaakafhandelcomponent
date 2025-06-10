/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.ontkoppeldedocumenten.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.documenten.model.OntkoppeldDocument
import nl.info.zac.app.identity.converter.RestUserConverter
import nl.info.zac.app.model.createRESTUser
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.model.createEnkelvoudigInformatieObjectLock
import nl.info.zac.model.createOntkoppeldDocument
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class RESTOntkoppeldDocumentConverterTest : BehaviorSpec({

    val userConverter = mockk<RestUserConverter>()
    val lockService = mockk<EnkelvoudigInformatieObjectLockService>()

    val converter = RESTOntkoppeldDocumentConverter(userConverter, lockService)

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("a valid OntkoppeldDocument and informatieobjectTypeUUID") {
        val uuid = UUID.randomUUID()
        val informatieobjectTypeUUID = UUID.randomUUID()
        val userId = "user-123"
        val ontkoppeldDocument = createOntkoppeldDocument(
            uuid = uuid,
            userId = userId
        )

        val convertedUser = createRESTUser(id = userId)
        val lock = createEnkelvoudigInformatieObjectLock(
            lock = "LOCKED"
        )

        every { userConverter.convertUserId(userId) } returns convertedUser
        every { lockService.findLock(uuid) } returns lock

        When("RESTOntkoppeldDocumentConverter is invoked") {
            val result = converter.convert(ontkoppeldDocument, informatieobjectTypeUUID)

            Then("the resulting RESTOntkoppeldDocument should end up with same values") {
                result.id shouldBe 1L
                result.documentUUID shouldBe uuid
                result.documentID shouldBe "DOC-456"
                result.informatieobjectTypeUUID shouldBe informatieobjectTypeUUID
                result.titel shouldBe "fakeTitel"
                result.zaakID shouldBe "ZAAK-001"
                result.creatiedatum shouldBe ontkoppeldDocument.creatiedatum
                result.bestandsnaam shouldBe "test.pdf"
                result.ontkoppeldDoor shouldBe convertedUser
                result.ontkoppeldOp shouldBe ontkoppeldDocument.ontkoppeldOp
                result.reden shouldBe "fakeReason"
                result.isVergrendeld shouldBe true
            }
        }
    }

    Given("the document is not locked") {
        val uuid = UUID.randomUUID()
        val userId = "user-456"
        val ontkoppeldDocument = OntkoppeldDocument().apply {
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

        When("RESTOntkoppeldDocumentConverter is invoked") {
            val result = converter.convert(ontkoppeldDocument, UUID.randomUUID())

            Then("isVergrendeld should be false") {
                result.isVergrendeld shouldBe false
            }
        }
    }
})
