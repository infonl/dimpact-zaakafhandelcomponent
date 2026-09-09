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
import io.mockk.verify
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
import nl.info.zac.smartdocuments.exception.SmartDocumentsConfigurationException
import nl.info.zac.smartdocuments.exception.SmartDocumentsUnsupportedOutputFormatException
import nl.info.zac.util.toBase64String
import java.net.URI
import java.util.Optional

class SmartDocumentsServiceTest : BehaviorSpec({
    val smartDocumentsURL = "https://example.com/fakeSmartDocumentsURL"
    val authenticationToken = "fakeAuthenticationToken"
    val fixedUserName = Optional.of("fakeFixedUserName")
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val smartDocumentsClient = mockk<Instance<SmartDocumentsClient>>()

    afterEach {
        checkUnnecessaryStub()
    }

    given("SmartDocuments is enabled") {
        val data = createData()
        val variables = Variables(
            outputFormats = listOf(OutputFormat("DOCX")),
            redirectMethod = "POST",
            redirectUrl = "url"
        )
        val smartDocument = createSmartDocument(variables)
        val attendedResponse = createAttendedResponse()
        every {
            smartDocumentsClient.get().attendedDeposit(any(), fixedUserName.get(), any())
        } returns attendedResponse

        val smartDocumentsService = SmartDocumentsService(
            smartDocumentsClient = smartDocumentsClient,
            enabled = Optional.of(true),
            smartDocumentsURL = Optional.of(smartDocumentsURL),
            authenticationToken = Optional.of(authenticationToken),
            loggedInUserInstance = loggedInUserInstance,
            fixedUserName = fixedUserName
        )

        `when`("the 'create document attended' method is called") {
            val documentCreationResponse = smartDocumentsService.createDocumentAttended(
                data = data,
                smartDocument = smartDocument
            )

            then(
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

    listOf(
        "abcd.docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "abcd.pdf" to "application/pdf",
        "abcd.odt" to "application/vnd.oasis.opendocument.text"
    ).forEach { (generatedFileName, expectedOutputFormat) ->
        given(
            "SmartDocuments is enabled and a document named '$generatedFileName' is generated and ready for download"
        ) {
            val smartDocumentId = "sdId"
            val downloadedFile = mockk<DownloadedFile>()
            val body = "body content".toByteArray(Charsets.UTF_8)

            every { smartDocumentsClient.get().downloadFile(smartDocumentId, null) } returns downloadedFile
            every { downloadedFile.body() } returns body
            every { downloadedFile.contentDisposition() } returns "attachment; filename=\"$generatedFileName\""

            val smartDocumentsService = SmartDocumentsService(
                smartDocumentsClient = smartDocumentsClient,
                enabled = Optional.of(true),
                smartDocumentsURL = Optional.of(smartDocumentsURL),
                authenticationToken = Optional.of(authenticationToken),
                loggedInUserInstance = loggedInUserInstance,
                fixedUserName = fixedUserName
            )

            `when`("the 'download file' method is called") {
                val file = smartDocumentsService.downloadDocument(smartDocumentId)

                then("no output format is requested and the file's format is derived from its extension") {
                    with(file) {
                        fileName shouldBe generatedFileName
                        outputFormat shouldBe expectedOutputFormat
                        document.data shouldBe body.toBase64String()
                    }
                    verify(exactly = 1) { smartDocumentsClient.get().downloadFile(smartDocumentId, null) }
                }
            }
        }
    }

    listOf("abcd.xml", "abcd.html").forEach { fileName ->
        given("SmartDocuments is enabled and a downloaded document named '$fileName' has an unsupported format") {
            val smartDocumentId = "sdId"
            val downloadedFile = mockk<DownloadedFile>()

            every { smartDocumentsClient.get().downloadFile(smartDocumentId, null) } returns downloadedFile
            every { downloadedFile.contentDisposition() } returns "attachment; filename=\"$fileName\""
            every { downloadedFile.body() } returns "body content".toByteArray(Charsets.UTF_8)

            val smartDocumentsService = SmartDocumentsService(
                smartDocumentsClient = smartDocumentsClient,
                enabled = Optional.of(true),
                smartDocumentsURL = Optional.of(smartDocumentsURL),
                authenticationToken = Optional.of(authenticationToken),
                loggedInUserInstance = loggedInUserInstance,
                fixedUserName = fixedUserName
            )

            `when`("the 'download file' method is called") {
                val exception = shouldThrow<SmartDocumentsUnsupportedOutputFormatException> {
                    smartDocumentsService.downloadDocument(smartDocumentId)
                }

                then("it fails with an error identifying the unsupported extension and file name") {
                    exception.message shouldBe
                        "Unsupported SmartDocuments output file extension: '.${fileName.substringAfterLast('.')}' " +
                        "for file name: '$fileName'"
                }
            }
        }
    }

    given("SmartDocuments is enabled and contains templates") {
        val templatesResponse = createsmartDocumentsTemplatesResponse()
        every {
            smartDocumentsClient.get().listTemplates(any(), fixedUserName.get())
        } returns templatesResponse

        val smartDocumentsService = SmartDocumentsService(
            smartDocumentsClient = smartDocumentsClient,
            enabled = Optional.of(true),
            smartDocumentsURL = Optional.of(smartDocumentsURL),
            authenticationToken = Optional.of(authenticationToken),
            loggedInUserInstance = loggedInUserInstance,
            fixedUserName = fixedUserName
        )

        `when`("list templates is called") {
            val templatesList = smartDocumentsService.listTemplates()

            then("it should return a list of templates") {
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

    given("SmartDocuments is enabled, no fixed user name is configured and a user is logged in") {
        val loggedInUser = createLoggedInUser()
        val templatesResponse = createsmartDocumentsTemplatesResponse()
        every { loggedInUserInstance.isUnsatisfied } returns false
        every { loggedInUserInstance.get() } returns loggedInUser
        every {
            smartDocumentsClient.get().listTemplates(any(), loggedInUser.id)
        } returns templatesResponse

        val smartDocumentsService = SmartDocumentsService(
            smartDocumentsClient = smartDocumentsClient,
            enabled = Optional.of(true),
            smartDocumentsURL = Optional.of(smartDocumentsURL),
            authenticationToken = Optional.of(authenticationToken),
            loggedInUserInstance = loggedInUserInstance,
            fixedUserName = Optional.empty()
        )

        `when`("list templates is called") {
            val templatesList = smartDocumentsService.listTemplates()

            then("it requests templates for the logged-in user") {
                templatesList shouldBe templatesResponse
            }
        }
    }

    given("SmartDocuments is enabled, no fixed user name is configured and no user is logged in") {
        every { loggedInUserInstance.isUnsatisfied } returns true

        val smartDocumentsService = SmartDocumentsService(
            smartDocumentsClient = smartDocumentsClient,
            enabled = Optional.of(true),
            smartDocumentsURL = Optional.of(smartDocumentsURL),
            authenticationToken = Optional.of(authenticationToken),
            loggedInUserInstance = loggedInUserInstance,
            fixedUserName = Optional.empty()
        )

        `when`("list templates is called") {
            val exception = shouldThrow<SmartDocumentsConfigurationException> {
                smartDocumentsService.listTemplates()
            }

            then("it throws an exception") {
                exception.message shouldBe
                    "No SmartDocuments fixed user name configured and no user is currently logged in"
            }
        }
    }

    given("SmartDocuments is disabled") {
        val smartDocumentsService = SmartDocumentsService(
            smartDocumentsClient = smartDocumentsClient,
            enabled = Optional.of(false),
            loggedInUserInstance = loggedInUserInstance,
            fixedUserName = fixedUserName
        )

        `when`("checking if enabled") {
            then("it returns `false`") {
                smartDocumentsService.isEnabled() shouldBe false
            }
        }
    }

    given("SmartDocuments state is not specified") {
        val smartDocumentsService = SmartDocumentsService(
            smartDocumentsClient = smartDocumentsClient,
            loggedInUserInstance = loggedInUserInstance,
            fixedUserName = fixedUserName
        )

        `when`("checking if enabled") {
            then("it returns `false`") {
                smartDocumentsService.isEnabled() shouldBe false
            }
        }
    }

    given("SmartDocuments is enabled, but not enough configuration is provided") {
        `when`("SmartDocumentsService is constructed") {
            val exception = shouldThrow<IllegalArgumentException> {
                SmartDocumentsService(
                    smartDocumentsClient = smartDocumentsClient,
                    enabled = Optional.of(true),
                    loggedInUserInstance = loggedInUserInstance,
                    fixedUserName = fixedUserName
                )
            }

            then("it throws an exception") {
                exception.message shouldBe "SMARTDOCUMENTS_CLIENT_MP_REST_URL environment variable required"
            }
        }
    }

    given("SmartDocuments is enabled and wizard authentication is disabled") {
        val data = createData()
        val variables = Variables(
            outputFormats = listOf(OutputFormat("DOCX")),
            redirectMethod = "POST",
            redirectUrl = "url"
        )
        val smartDocument = createSmartDocument(variables)
        val attendedResponse = createAttendedResponse()

        every {
            smartDocumentsClient.get().attendedDepositNoAuth(any(), any())
        } returns attendedResponse

        val smartDocumentsService = SmartDocumentsService(
            smartDocumentsClient = smartDocumentsClient,
            enabled = Optional.of(true),
            smartDocumentsURL = Optional.of(smartDocumentsURL),
            authenticationToken = Optional.of(authenticationToken),
            loggedInUserInstance = loggedInUserInstance,
            fixedUserName = fixedUserName,
            wizardAuthEnabled = Optional.of(false)
        )

        `when`("the 'createDocumentAttended' method is called without authorisation") {
            val response = smartDocumentsService.createDocumentAttended(
                data = data,
                smartDocument = smartDocument
            )

            then("it calls the attendedDepositNoAuth and returns the response") {
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
