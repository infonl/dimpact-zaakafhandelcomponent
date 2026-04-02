/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.informatieobjecten

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import nl.info.client.officeconverter.OfficeConverterClientService
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectWithLockRequest
import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.zac.app.informatieobjecten.exception.EnkelvoudigInformatieObjectConversionException
import nl.info.zac.util.toBase64String
import java.io.ByteArrayInputStream
import java.util.UUID

class EnkelvoudigInformatieObjectConvertServiceTest : BehaviorSpec({
    val drcClientService = mockk<DrcClientService>()
    val officeConverterClientService = mockk<OfficeConverterClientService>()
    val enkelvoudigInformatieObjectUpdateService = mockk<EnkelvoudigInformatieObjectUpdateService>()
    val service = EnkelvoudigInformatieObjectConvertService(
        drcClientService,
        officeConverterClientService,
        enkelvoudigInformatieObjectUpdateService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("a document with status DEFINITIEF") {
        val uuid = UUID.randomUUID()
        val document = createEnkelvoudigInformatieObject(uuid = uuid).apply {
            status = StatusEnum.DEFINITIEF
            bestandsnaam = "rapport.docx"
        }
        val documentBytes = "document content".toByteArray()
        val pdfBytes = "pdf content".toByteArray()
        val requestSlot = slot<EnkelvoudigInformatieObjectWithLockRequest>()

        every { drcClientService.downloadEnkelvoudigInformatieobject(uuid) } returns
            ByteArrayInputStream(documentBytes)
        every {
            officeConverterClientService.convertToPDF(any<ByteArrayInputStream>(), document.bestandsnaam)
        } returns ByteArrayInputStream(pdfBytes)
        every {
            enkelvoudigInformatieObjectUpdateService.updateEnkelvoudigInformatieObjectWithLockData(
                uuid, capture(requestSlot), "Geconverteerd naar PDF"
            )
        } returns createEnkelvoudigInformatieObject()

        When("convertEnkelvoudigInformatieObjectToPDF is called") {
            service.convertEnkelvoudigInformatieObjectToPDF(document, uuid)

            Then("the document is downloaded and converted to PDF") {
                verify(exactly = 1) { drcClientService.downloadEnkelvoudigInformatieobject(uuid) }
                verify(exactly = 1) {
                    officeConverterClientService.convertToPDF(any<ByteArrayInputStream>(), "rapport.docx")
                }
            }

            And("the update is called with a PDF request containing the correct fields") {
                verify(exactly = 1) {
                    enkelvoudigInformatieObjectUpdateService.updateEnkelvoudigInformatieObjectWithLockData(
                        uuid, any(), "Geconverteerd naar PDF"
                    )
                }
                with(requestSlot.captured) {
                    inhoud shouldBe pdfBytes.toBase64String()
                    formaat shouldBe "application/pdf"
                    bestandsnaam shouldBe "rapport.pdf"
                    bestandsomvang shouldBe pdfBytes.size
                }
            }
        }
    }

    Given("a document with status other than DEFINITIEF") {
        val uuid = UUID.randomUUID()
        val document = createEnkelvoudigInformatieObject(uuid = uuid).apply {
            status = StatusEnum.IN_BEWERKING
        }

        When("convertEnkelvoudigInformatieObjectToPDF is called") {
            val thrownException = runCatching {
                service.convertEnkelvoudigInformatieObjectToPDF(document, uuid)
            }.exceptionOrNull()

            Then("an EnkelvoudigInformatieObjectConversionException is thrown") {
                thrownException.shouldBeInstanceOf<EnkelvoudigInformatieObjectConversionException>()
            }
        }
    }
})
