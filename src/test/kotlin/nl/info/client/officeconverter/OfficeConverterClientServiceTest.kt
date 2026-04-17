/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.officeconverter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.ws.rs.core.Response
import nl.info.client.officeconverter.exception.MessageEntityDataCouldNotBeBufferedException
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput
import java.io.ByteArrayInputStream

class OfficeConverterClientServiceTest : BehaviorSpec({
    val response = mockk<Response>()
    val officeConverterClient = mockk<OfficeConverterClient>()
    val officeConverterClientService = OfficeConverterClientService(officeConverterClient)

    Context("Converting a document to PDF") {
        Given("A byte array input stream and an office converter client response that could be buffered") {
            val document = ByteArrayInputStream("fakeDocumentContent".toByteArray())
            val filename = "fakeFilename.docx"
            val expectedPdf = ByteArrayInputStream("fakePdfContent".toByteArray())
            val multipartSlot = slot<MultipartFormDataOutput>()
            every { response.bufferEntity() } returns true
            every { response.entity } returns expectedPdf
            every { officeConverterClient.convert(capture(multipartSlot)) } returns response

            When("convertToPDF is called") {
                val result = officeConverterClientService.convertToPDF(document, filename)

                Then("it returns the PDF as a ByteArrayInputStream") {
                    val resultBytes = result.readAllBytes()
                    resultBytes shouldBe "fakePdfContent".toByteArray()
                    verify(exactly = 1) { officeConverterClient.convert(any()) }
                }

                Then("the multipart form data contains a part named 'files' matching the Gotenberg API contract") {
                    val formDataMap = multipartSlot.captured.formDataMap
                    formDataMap shouldContainKey "files"
                    formDataMap shouldNotContainKey "file"
                }
            }
        }

        Given("A byte array input stream and an office converter client response that could not be buffered") {
            val document = ByteArrayInputStream("fakeDocumentContent".toByteArray())
            val filename = "fakeFilename.docx"
            every { response.bufferEntity() } returns false
            every { officeConverterClient.convert(any()) } returns response

            When("convertToPDF is called") {
                val exception = shouldThrow<MessageEntityDataCouldNotBeBufferedException> {
                    officeConverterClientService.convertToPDF(document, filename)
                }

                Then("it throws a RuntimeException with an explanatory message") {
                    exception.message shouldBe "Content of PDF converter could not be buffered."
                    verify(exactly = 1) { officeConverterClient.convert(any()) }
                }
            }
        }
    }
})
