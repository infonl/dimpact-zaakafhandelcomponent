/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.smartdocuments

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import nl.info.client.smartdocuments.SmartDocumentsClient
import nl.info.client.smartdocuments.model.createAttendedResponse
import nl.info.client.smartdocuments.model.createSmartDocument
import nl.info.client.smartdocuments.model.createsmartDocumentsTemplatesResponse
import nl.info.client.smartdocuments.model.document.OutputFormat
import nl.info.client.smartdocuments.model.document.Variables
import nl.info.client.smartdocuments.rest.DownloadedFile
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.documentcreation.model.createData
import nl.info.zac.util.toBase64String
import java.net.URI
import java.util.Optional

class SmartDocumentsServiceTest : BehaviorSpec({
    val smartDocumentsURL = "https://example.com/fakeSmartDocumentsURL"
    val authenticationToken = "fakeAuthenticationToken"
    val fixedUserName = Optional.of("fakeFixedUserName")
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val smartDocumentsClient = mockk<Instance<SmartDocumentsClient>>()

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("SmartDocuments is enabled") {
        val loggedInUser = createLoggedInUser()
        val data = createData()
        val variables = Variables(
            outputFormats = listOf(OutputFormat("DOCX")),
            redirectMethod = "POST",
            redirectUrl = "url"
        )
        val smartDocument = createSmartDocument(variables)
        val attendedResponse = createAttendedResponse()
        every { loggedInUserInstance.get() } returns loggedInUser
        every { smartDocumentsClient.get().attendedDeposit(any(), any(), any()) } returns attendedResponse

        val smartDocumentsService = SmartDocumentsService(
            smartDocumentsClient = smartDocumentsClient,
            enabled = Optional.of(true),
            smartDocumentsURL = Optional.of(smartDocumentsURL),
            authenticationToken = Optional.of(authenticationToken),
            loggedInUserInstance = loggedInUserInstance,
            fixedUserName = fixedUserName
        )

        When("the 'create document attended' method is called") {
            val documentCreationResponse = smartDocumentsService.createDocumentAttended(
                data = data,
                smartDocument = smartDocument
            )

            Then(
                """
                the attended SmartDocuments document creation wizard is started and a document creation response is returned
                """
            ) {
                with(documentCreationResponse) {
                    redirectUrl shouldBe URI(
                        "$smartDocumentsURL/smartdocuments/wizard?ticket=${attendedResponse.ticket}"
                    )
                    message shouldBe null
                }
            }
        }
    }

    Given("SmartDocuments is enabled and a document is generated and ready for download") {
        val downloadedFile = mockk<DownloadedFile>()

        val fileName = "abcd.docx"
        val body = "body content".toByteArray(Charsets.UTF_8)

        every { smartDocumentsClient.get().downloadFile(any(), any()) } returns downloadedFile
        every { downloadedFile.body() } returns body
        every { downloadedFile.contentDisposition() } returns "attachment; filename=\"$fileName\""

        val smartDocumentsService = SmartDocumentsService(
            smartDocumentsClient = smartDocumentsClient,
            enabled = Optional.of(true),
            smartDocumentsURL = Optional.of(smartDocumentsURL),
            authenticationToken = Optional.of(authenticationToken),
            loggedInUserInstance = loggedInUserInstance,
            fixedUserName = fixedUserName
        )

        When("the 'download file' method is called") {
            val file = smartDocumentsService.downloadDocument("sdId")

            Then("a file object representing the content is returned") {
                with(file) {
                    fileName shouldBe fileName
                    outputFormat shouldBe "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    document.data shouldBe body.toBase64String()
                }
            }
        }
    }

    Given("SmartDocuments is enabled and contains templates") {
        val loggedInUser = createLoggedInUser()
        every { loggedInUserInstance.get() } returns loggedInUser

        val templatesResponse = createsmartDocumentsTemplatesResponse()
        every {
            smartDocumentsClient.get().listTemplates(any(), any())
        } returns templatesResponse

        val smartDocumentsService = SmartDocumentsService(
            smartDocumentsClient = smartDocumentsClient,
            enabled = Optional.of(true),
            smartDocumentsURL = Optional.of(smartDocumentsURL),
            authenticationToken = Optional.of(authenticationToken),
            loggedInUserInstance = loggedInUserInstance,
            fixedUserName = fixedUserName
        )

        When("list templates is called") {
            val templatesList = smartDocumentsService.listTemplates()

            Then("it should return a list of templates") {
                with(templatesList.documentsStructure.templatesStructure.templateGroups) {
                    size shouldBe 1
                    with(first()) {
                        name shouldBe "Dimpact"
                        templateGroups!!.size shouldBe 2
                        templateGroups!!.first().name shouldBe "Intern zaaktype voor test volledig gebruik ZAC"
                        templates!!.size shouldBe 2
                        templates!!.first().name shouldBe "Aanvullende informatie nieuw"
                    }
                }
            }
        }
    }

    Given("SmartDocuments is disabled") {
        val smartDocumentsService = SmartDocumentsService(
            smartDocumentsClient = smartDocumentsClient,
            enabled = Optional.of(false),
            loggedInUserInstance = loggedInUserInstance,
            fixedUserName = fixedUserName
        )

        When("checking if enabled") {
            Then("it returns `false`") {
                smartDocumentsService.isEnabled() shouldBe false
            }
        }
    }

    Given("SmartDocuments state is not specified") {
        val smartDocumentsService = SmartDocumentsService(
            smartDocumentsClient = smartDocumentsClient,
            loggedInUserInstance = loggedInUserInstance,
            fixedUserName = fixedUserName
        )

        When("checking if enabled") {
            Then("it returns `false`") {
                smartDocumentsService.isEnabled() shouldBe false
            }
        }
    }

    Given("SmartDocuments is enabled, but not enough configuration is provided") {
        When("SmartDocumentsService is constructed") {
            val exception = shouldThrow<IllegalArgumentException> {
                SmartDocumentsService(
                    smartDocumentsClient = smartDocumentsClient,
                    enabled = Optional.of(true),
                    loggedInUserInstance = loggedInUserInstance,
                    fixedUserName = fixedUserName
                )
            }

            Then("it throws an exception") {
                exception.message shouldBe "SMARTDOCUMENTS_CLIENT_MP_REST_URL environment variable required"
            }
        }
    }

    Given("SmartDocuments is enabled and wizard authentication is disabled") {
        val loggedInUser = createLoggedInUser()
        val data = createData()
        val variables = Variables(
            outputFormats = listOf(OutputFormat("DOCX")),
            redirectMethod = "POST",
            redirectUrl = "url"
        )
        val smartDocument = createSmartDocument(variables)
        val attendedResponse = createAttendedResponse()

        every { loggedInUserInstance.get() } returns loggedInUser

        every {
            smartDocumentsClient.get().attendedDepositNoAuth(any(), any())
        } returns attendedResponse

        every {
            smartDocumentsClient.get().attendedDeposit(any(), any(), any())
        } throws AssertionError("Should not call attendedDeposit when wizardAuthEnabled is false")

        val smartDocumentsService = SmartDocumentsService(
            smartDocumentsClient = smartDocumentsClient,
            enabled = Optional.of(true),
            smartDocumentsURL = Optional.of(smartDocumentsURL),
            authenticationToken = Optional.of(authenticationToken),
            loggedInUserInstance = loggedInUserInstance,
            fixedUserName = fixedUserName,
            wizardAuthEnabled = Optional.of(false)
        )

        When("the 'createDocumentAttended' method is called without authorisation") {
            val response = smartDocumentsService.createDocumentAttended(
                data = data,
                smartDocument = smartDocument
            )

            Then("it calls the attendedDepositNoAuth and returns the response") {
                with(response) {
                    redirectUrl shouldBe URI(
                        "$smartDocumentsURL/smartdocuments/wizard?ticket=${attendedResponse.ticket}"
                    )
                    message shouldBe null
                }
            }
        }
    }
})
