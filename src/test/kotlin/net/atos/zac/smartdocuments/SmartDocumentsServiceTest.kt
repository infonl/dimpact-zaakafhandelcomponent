/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.smartdocuments

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import net.atos.client.smartdocuments.SmartDocumentsClient
import net.atos.client.smartdocuments.model.createAttendedResponse
import net.atos.client.smartdocuments.model.createSmartDocument
import net.atos.client.smartdocuments.model.createsmartDocumentsTemplatesResponse
import net.atos.client.smartdocuments.model.document.OutputFormat
import net.atos.client.smartdocuments.model.document.Variables
import net.atos.client.smartdocuments.rest.DownloadedFile
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.authentication.createLoggedInUser
import net.atos.zac.documentcreation.model.createData
import nl.info.zac.util.toBase64String
import java.net.URI
import java.util.Optional

class SmartDocumentsServiceTest : BehaviorSpec({
    val smartDocumentsURL = "https://example.com/dummySmartDocumentsURL"
    val authenticationToken = "dummyAuthenticationToken"
    val fixedUserName = Optional.of("dummyFixedUserName")
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val smartDocumentsClient = mockk<SmartDocumentsClient>()

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
        every { smartDocumentsClient.attendedDeposit(any(), any(), any()) } returns attendedResponse

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

        every { smartDocumentsClient.downloadFile(any(), any()) } returns downloadedFile
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
            smartDocumentsClient.listTemplates(any(), any())
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

})
